package com.shackshake.fo4squardmaker.util;

import com.shackshake.fo4squardmaker.entity.*;
import com.shackshake.fo4squardmaker.model.util.PlayerModel;
import com.shackshake.fo4squardmaker.repository.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * 데이터 세팅
 * - 포지션
 * - 시즌
 * - 선수 정보
 */

@ExtendWith(SpringExtension.class)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)// Im memory 외 DB에 접근시 사용
//@SpringBootTest
class StaticDataLoaderTest {
    private final static long SALAH_PID = 209331L;
    private final static long RAMOS_PID = 155862L;
    private final static long PID_MOD = 1000000L;

    private final static String PLAYER_DETAIL_URL = "https://fifaonline4.inven.co.kr/dataninfo/player/?code=";

//    @PersistenceContext EntityManager em;

    @Autowired private PositionRepository positionRepository;
    @Autowired private SeasonRepository seasonRepository;
    @Autowired private PlayerRepository playerRepository;
//    @Autowired private ClubRepository clubRepository;
//    @Autowired private PlayerClubRepository playerClubRepository;

    @Test
    void savePositions() {
        final String[] positionNames = {"GK", "SW", "RWB", "RB", "RCB", "CB", "LCB", "LB", "LWB",
                "RDM", "CDM", "LDM", "RM", "RCM", "CM", "LCM", "LM", "RAM", "CAM", "LAM",
                "RF", "CF", "LF", "RW", "RS", "ST", "LS", "LW"};
        for (String name : positionNames) {
            positionRepository.save(Position.builder()
                    .name(name)
                    .build());
        }

        assertThat(positionNames.length).isEqualTo(positionRepository.count());
    }

    @Test
    void saveSeason() throws IOException { // 피파4와 인벤의 season_id가 다름
        // 인벤 season
        final String INVEN_SEASON_URL = "https://fifaonline4.inven.co.kr/dataninfo/player/";
        Document document = Jsoup.connect(INVEN_SEASON_URL).get();
        Elements elements = document.select("div.fifa4.value.season.clearfix").select("label.checkbox");
//        Elements elements = document.select("div.fifa4.value.season.clearfix label.checkbox");

        int registerCnt = 0;
        for (Element element : elements) {
            long seasonId = Long.parseLong(element.getElementsByTag("input").attr("value"));
            String className = element.getElementsByTag("span").text();

            ///////     인벤과 피파의 season_id가 다른 경우     ////////
            seasonId = getFo4SeasonId(seasonId);
            ///////////////////////////////////////////////////////

            seasonRepository.save(Season.builder()
                                        .id(seasonId)
                                        .name(className)
                                        .build());
            registerCnt++;
        }

        assertThat(seasonRepository.count()).isEqualTo(registerCnt);
        assertThat(seasonRepository.findById(234L).isPresent());
        assertThat(seasonRepository.findById(234L).get().getName()).isEqualTo("LH");
    }

    long getFo4SeasonId(long seansonId) {
        if (seansonId == 203L) seansonId = 212L;
        else if (seansonId == 224L) seansonId = 234L;

        return seansonId;
    }

    @Test
    void savePlayers() throws IOException {
//        UriComponents uriComponents= UriComponentsBuilder.fromHttpUrl(SPID_JSON_URL).build();
//        HttpHeaders headers = new HttpHeaders();

//        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
//        factory.setConnectionRequestTimeout(10000);
//        factory.setReadTimeout(10000);
        final String SPID_JSON_URL = "https://static.api.nexon.co.kr/fifaonline4/latest/spid.json";
        final String INVEN_PLAYER_LIST_IMAGE_PREFIX = "https://static.inven.co.kr/image_2011/site_image/fifaonline4/playericon/p";
        final String INVEN_PLAYER_LIST_IMAGE_SURFIX = ".png?v=211213a";

        RestTemplate restTemplate = new RestTemplate();
//        List<Player> playerList = restTemplate.getForObject(SPID_JSON_URL, List.class); // for문으로 배열 요소 접근시 오류 발생
        ResponseEntity<List<PlayerModel>> response = restTemplate.exchange(SPID_JSON_URL, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
        List<PlayerModel> playerList = response.getBody();


        // DB에 살라, 라모스 등록
        int registerCnt = 0;
        for (PlayerModel playerModel : playerList) {
            long spid = playerModel.getId();
            String name = playerModel.getName();

            long pid = spid % PID_MOD;
            if (!(pid == SALAH_PID|| pid == RAMOS_PID)) continue;

            long invenPlayerId = getInvenSeasonId(spid / PID_MOD) * PID_MOD + pid;

            // 급여(pay 획득을 위한) 크롤링 진행
            UriComponents uriComponents= UriComponentsBuilder.fromHttpUrl(PLAYER_DETAIL_URL)
                    .queryParam("code", invenPlayerId)
                    .build();

            Document document = Jsoup.connect(uriComponents.toString()).get();
            int pay = Integer.parseInt(document.select("ul.fifa4.state.clearfix").select("b.pay").attr("data-ori"));

            // 이미지 경로 확인
            boolean isSpidImgSrc = true;

            StringBuilder sb = new StringBuilder();
            sb.append(INVEN_PLAYER_LIST_IMAGE_PREFIX).append("a").append(invenPlayerId).append(INVEN_PLAYER_LIST_IMAGE_SURFIX);
            URL url = new URL(sb.toString());
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            if (con.getResponseCode() == 404) {
                sb = new StringBuilder();
                sb.append(INVEN_PLAYER_LIST_IMAGE_PREFIX).append(pid).append(INVEN_PLAYER_LIST_IMAGE_SURFIX);

                url = new URL(sb.toString());
                con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");

                isSpidImgSrc = false;
            }

            assertThat(con.getResponseCode()).isEqualTo(200);

            Player player = Player.builder()
                                .id(spid)
                                .name(name)
                                .pay(pay)
                                .isSpidImgSrc(isSpidImgSrc)
                                .build();

            playerRepository.save(player);
            registerCnt++;
        }

        assertThat(playerRepository.count()).isEqualTo(registerCnt);
    }

    long getInvenSeasonId(long seansonId) {
        if (seansonId == 212L) seansonId = 203L;
        else if (seansonId == 234L) seansonId = 224L;

        return seansonId;
    }

//    @Test
//    void getClubsOfPlayer() throws IOException {// 특정 선수의 팀명 불러오기
//        Player player = playerRepository.findById(250209331L).get();// findById하면 Optional로 나옴
//
//        String[] validclubs = {"리버풀", "로마 FC", "첼시", "FC 바젤 1893", "엘모카울룬 알아랍"};// 검증용
//        final String LOAN = "(임대)";
//
//        UriComponents uriComponents= UriComponentsBuilder.fromHttpUrl(PLAYER_DETAIL_URL)
//                .queryParam("code", player.getId())
//                .build();
//
//        Document document = Jsoup.connect(uriComponents.toString()).get();
//        Elements clubsOfPlayerHTML = document.select("article section.commu-center div.commu-body.pcMain div.fifa4.legacyDbPage div.fifa4.player_club.clearfix ul.fifa4.list.clearfix li");
//
//        String closedSpan = "</span> ";
//        String closedLi = " </li>";
//
//        int idx = 0;
//        for (Element clubWithYearHTML : clubsOfPlayerHTML) {
//            String clubWithYear = clubWithYearHTML.toString();
//            if (clubWithYear.contains(LOAN)) continue;
//
//            int s = clubWithYear.indexOf(closedSpan) + closedSpan.length();
//            int d = clubWithYear.indexOf(closedLi);
//
//            String club = clubWithYear.substring(s, d);
//            System.out.println(club);
//            assertThat(club).isEqualTo(validclubs[idx++]);
//        }
//    }
//
//    @Test
//    void mappingPlayerAndClub() {
//        // 살라로 테스트
//        final String[] CLUBS_OF_SALAH = {"리버풀", "로마 FC", "첼시", "FC 바젤 1893", "엘모카울룬 알아랍"};
//
//        String jpql = "SELECT p FROM Player p WHERE (ID % 1000000L) = :pid";
//        TypedQuery<Player> query = em.createQuery(jpql, Player.class);
//        query.setParameter("pid", SALAH_PID);
//
//        List<Player> playerWithDifferentSeasonList = query.getResultList();// 왜 jpql은 Optional로 리턴 안되는지?
//        for (String clubOfPlayer : CLUBS_OF_SALAH) {
//            Optional<Club> clubOptional = clubRepository.findByName(clubOfPlayer);
//            Club club ;
//
//            if (!clubOptional.isPresent()) {
//                club = clubRepository.save(Club.builder()
//                        .name(clubOfPlayer)
//                        .build());
//            } else club = clubOptional.get();
//
//            for (Player player : playerWithDifferentSeasonList) {
//                playerClubRepository.save(PlayerClub.builder()
//                        .player(player)// 객체가 들어가야함
//                        .club(club)// 객체가 들어가야함
//                        .build());
//            }
//        }
//    }
}