package com.shackshake.fo4squardmaker.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shackshake.fo4squardmaker.entity.Club;
import com.shackshake.fo4squardmaker.entity.Player;
import com.shackshake.fo4squardmaker.entity.PlayerClub;
import com.shackshake.fo4squardmaker.repository.ClubRepository;
import com.shackshake.fo4squardmaker.repository.PlayerClubRepository;
import com.shackshake.fo4squardmaker.repository.PlayerRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * - 선수 고유 식별자(spid) 메타데이터 조회 -> spid, name
 * - 포지션
 * - 시즌
 *
 * 생각해보기
 * - @DataJpaTest에서 ApplicationContext 오류나는거 뭔지..
 * - restTemplate 그냥 List로 받으면 컴파일러가 LinkedHashMap으로 받고 오류남..
 */

//@ExtendWith(SpringExtension.class)
//@DataJpaTest
@SpringBootTest
class StaticDataLoaderTest {
    private final static long SALAH_PID = 209331L;
    private final static long RAMOS_PID = 155862L;
    private final static long PID_MOD = 1000000L;

    private final static String PLAYER_DETAIL_URL = "https://fifaonline4.inven.co.kr/dataninfo/player/?code=";

    @PersistenceContext EntityManager em;

    @Autowired private PlayerRepository playerRepository;
    @Autowired private ClubRepository clubRepository;
    @Autowired private PlayerClubRepository playerClubRepository;

    @Test
    void loadAndSaveSelectedSpidJson() {
//        UriComponents uriComponents= UriComponentsBuilder.fromHttpUrl(SPID_JSON_URL).build();
//        HttpHeaders headers = new HttpHeaders();

//        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
//        factory.setConnectionRequestTimeout(10000);
//        factory.setReadTimeout(10000);
        final String SPID_JSON_URL = "https://static.api.nexon.co.kr/fifaonline4/latest/spid.json";
        final int PLAYERS_NUM = 51048;
        RestTemplate restTemplate = new RestTemplate();
//        List<Player> playerList = restTemplate.getForObject(SPID_JSON_URL, List.class); // 왜 안되는지
        ResponseEntity<List<Player>> response = restTemplate.exchange(SPID_JSON_URL, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
        List<Player> playerList = response.getBody();

        assertThat(playerList.size()).isEqualTo(PLAYERS_NUM);

        // DB에 살라, 라모스 등록
        for (Player player : playerList) {
            long pid = player.getId() % PID_MOD;

            if (!(pid == SALAH_PID|| pid == RAMOS_PID)) continue;

            System.out.println(player.getName());
            playerRepository.save(player);
        }
    }

    @Test
    void getClubsOfPlayer() throws IOException {// 특정 선수의 팀명 불러오기
        Player player = playerRepository.findById(250209331L).get();// findById하면 Optional로 나옴

        String[] validclubs = {"리버풀", "로마 FC", "첼시", "FC 바젤 1893", "엘모카울룬 알아랍"};// 검증용
        final String LOAN = "(임대)";

        UriComponents uriComponents= UriComponentsBuilder.fromHttpUrl(PLAYER_DETAIL_URL)
                .queryParam("code", player.getId())
                .build();

        Document document = Jsoup.connect(uriComponents.toString()).get();
        Elements clubsOfPlayerHTML = document.select("article section.commu-center div.commu-body.pcMain div.fifa4.legacyDbPage div.fifa4.player_club.clearfix ul.fifa4.list.clearfix li");

        String closedSpan = "</span> ";
        String closedLi = " </li>";

        int idx = 0;
        for (Element clubWithYearHTML : clubsOfPlayerHTML) {
            String clubWithYear = clubWithYearHTML.toString();
            if (clubWithYear.contains(LOAN)) continue;

            int s = clubWithYear.indexOf(closedSpan) + closedSpan.length();
            int d = clubWithYear.indexOf(closedLi);

            String club = clubWithYear.substring(s, d);
            System.out.println(club);
            assertThat(club).isEqualTo(validclubs[idx++]);
        }
    }

    @Test
    void mappingPlayerAndClub() {
        // 살라로 테스트
        final String[] CLUBS_OF_SALAH = {"리버풀", "로마 FC", "첼시", "FC 바젤 1893", "엘모카울룬 알아랍"};

        String jpql = "SELECT p FROM Player p WHERE (ID % 1000000L) = :pid";
        TypedQuery<Player> query = em.createQuery(jpql, Player.class);
        query.setParameter("pid", SALAH_PID);

        List<Player> playerWithDifferentSeasonList = query.getResultList();// 왜 jpql은 Optional로 리턴 안되는지?
        for (String clubOfPlayer : CLUBS_OF_SALAH) {
            Optional<Club> clubOptional = clubRepository.findByName(clubOfPlayer);
            Club club ;

            if (!clubOptional.isPresent()) {
                club = clubRepository.save(Club.builder()
                        .name(clubOfPlayer)
                        .build());
            } else club = clubOptional.get();

            for (Player player : playerWithDifferentSeasonList) {
                playerClubRepository.save(PlayerClub.builder()
                        .player(player)// 객체가 들어가야함
                        .club(club)// 객체가 들어가야함
                        .build());
            }
        }

    }

//    @Test
//    void fillSelectedPlayerRemainedAttr() {
//        // Player에서 안채워진 나머지 속성(ovr, pay, isSpidImgSrc) 확인 -> div.fifa4.player_info.clearfix
//
//        final String PLAYER_LIST_URL = "https://fifaonline4.inven.co.kr/dataninfo/player/?mode=getList&team=%EB%A6%AC%EB%B2%84%ED%92%80&searchword=%EC%82%B4%EB%9D%BC";
//        UriComponents uriComponents= UriComponentsBuilder.fromHttpUrl(PLAYER_LIST_URL)
//                                                        .queryParam("team", )
//                                                        .build();
//
//        Document document = Jsoup.connect(searchPlayerTestURL).get();
//        Elements clubsOfPlayerHTML = document.select("article section.commu-center div.commu-body.pcMain div.fifa4.legacyDbPage div.fifa4.player_club.clearfix ul.fifa4.list.clearfix li");
//    }
}