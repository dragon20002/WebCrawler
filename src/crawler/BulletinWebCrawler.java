package crawler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;

public class BulletinWebCrawler {

	public interface ParsingMethod {
		public String getBulletinUrl(int page);
		
		public List<String> parseBulletinPage(Element body);
		
		public List<String> parseContent(Element body);

		public boolean isFinished(int page);
	}

	public interface CrawlingCallback {
		public void onNext(List<String> strList);

		public void onComplete(List<List<String>> result);
	}

	private ParsingMethod parsing;
	private CrawlingCallback callback;
	private List<List<String>> result = new ArrayList<>();

	public BulletinWebCrawler(ParsingMethod parsing, CrawlingCallback callback) {
		this.parsing = parsing;
		this.callback = callback;
	}

	public void start() {

		Observable<List<String>> crawling$ = Observable.create(emitter -> {
			crawling(1, emitter);
		});

		crawling$.subscribe(strList -> { // add
			callback.onNext(strList);
			result.add(strList);
		}, err -> { // empty
		}, () -> { // save
			callback.onComplete(result);
		});
	}

	private void crawling(int page, ObservableEmitter<List<String>> crawlObs) {
		String bulletinUrl = parsing.getBulletinUrl(page);

		try {
			Document doc = Jsoup.connect(bulletinUrl).get();
			List<String> urls = parsing.parseBulletinPage(doc.body());
			for (String url : urls) {
				Document urlDoc = Jsoup.connect(url).get();
				List<String> strList = parsing.parseContent(urlDoc.body());
				crawlObs.onNext(strList);
				
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		if (parsing.isFinished(page))
			crawlObs.onComplete();
	}

}
