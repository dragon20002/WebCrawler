import java.io.IOException;

import parser.ExcelModelExtractor;
import parser.NeedEssenceWebParser;

public class Main {
	
	public static void main(String[] args) {
		if (args.length == 0) {
			System.out.println("usage: program_name xlsx_file_name");
			return;
		}
		System.out.println(args[0]);

		try {
			new ExcelModelExtractor().extract(args[0], args[1]);
		} catch (IOException e) {
			System.out.println("no excel file");
		}
		
//		crawlingWiki();
	}

	private static void crawlingWiki() {
		Thread th = new Thread(() -> {
			NeedEssenceWebParser parser = new NeedEssenceWebParser();
			parser.extract();
			
			while (!parser.isFinished) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});

		th.start();
		try {
			th.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
