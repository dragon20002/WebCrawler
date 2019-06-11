package parser;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import crawler.BulletinWebCrawler;

public class NeedEssenceWebParser {
	public boolean isFinished = false;

	public void extract() {
		new BulletinWebCrawler(new BulletinWebCrawler.ParsingMethod() {

			@Override
			public List<String> parseBulletinPage(Element body) {
				List<String> urls = new ArrayList<>();
				
				Elements elems = body.getElementsByClass("wikitable");
				for (int i = 29; i < elems.size(); i++) {
					Element elem = elems.get(i);
					
					String url = "http://wiki.joyme.com" + elem.getElementsByTag("a").get(1).attr("href");
					urls.add(url);
					log("parsePage: " + i + ":" + url);
				}

				return urls;
			}

			@Override
			public List<String> parseContent(Element body) {
				List<String> contents = new ArrayList<>();
				
				String witchName = body.getElementsByClass("selflink").get(0).text();
				contents.add(witchName);

				Elements elems = body.getElementsByClass("iteminfo");
				for (int i = 0; i < elems.size(); i++) {
					Element elem = elems.get(i);
					
					String essenceName = elem.getElementsByTag("a").get(0).text();
					contents.add(essenceName);
				}

				return contents;
			}

			@Override
			public boolean isFinished(int page) {
				return true;
			}

			@Override
			public String getBulletinUrl(int page) {
				return "http://wiki.joyme.com/mnbq/%E9%AD%94%E5%A5%B3%E4%B8%80%E8%A7%88";
			}
		}, new BulletinWebCrawler.CrawlingCallback() {

			@Override
			public void onNext(List<String> strList) {
				for (String str : strList) {
					System.out.print(str + ", ");
				}
				System.out.println();
			}

			@Override
			public void onComplete(List<List<String>> result) {
				isFinished = true;
			}
		}).start();
	}

	private void log(String msg) {
		System.out.println("[" + new Date() + "] : " + msg);
	}
}
