package com.shackshake.fo4squardmaker.crawling;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

public class CrawlingTest {
    static String spid = "250209331";
    static String searchPlayerTestURL = "https://fifaonline4.inven.co.kr/dataninfo/player/?code="+spid;//21TOTY 살라

    @Test
    void getClubsOfPlayer() throws IOException {// 효율성 높일 수 있는 방법 고민
        String[] validclubs = {"리버풀", "로마 FC", "첼시", "FC 바젤 1893", "엘모카울룬 알아랍"};
        final String LOAN = "(임대)";

        Document document = Jsoup.connect(searchPlayerTestURL).get();
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
            assertThat(club.equals(validclubs[idx++]));
        }
    }
}
