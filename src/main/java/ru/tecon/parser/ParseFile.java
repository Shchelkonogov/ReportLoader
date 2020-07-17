package ru.tecon.parser;

import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import ru.tecon.Utils;
import ru.tecon.parser.model.ReportData;
import ru.tecon.parser.types.*;
import ru.tecon.parser.types.html.HTMLType;
import ru.tecon.parser.types.xml.XMLType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ParseFile {
	
	private static TikaConfig tikaConfig = TikaConfig.getDefaultConfig();

	private ParseFile() {
	}

	public static ReportData parseFile(String filePath) throws ParseException {
		String text;
		switch (Utils.getExtension(filePath)) {
			case "pdf": {
				text = getText(Paths.get(filePath));
				if (Type7.checkType(text)) {
					return Type7.getData(text, filePath);
				}
				if (Type5.checkType(text)) {
					return Type5.getData(text, filePath);
				}
				if (Type3.checkType(filePath)) {
					return Type3.getData(filePath);
				}
				if (Type6.checkType(filePath)) {
					return Type6.getData(filePath);
				}
				if (Type8.checkType(text)) {
					return Type8.getData(text, filePath);
				}
				if (Type9.checkType(filePath)) {
					return Type9.getData(filePath);
				}
				if (Type10.checkType(text)) {
					return Type10.getData(text, filePath);
				}
				throw new ParseException("Неизвестный шаблон pdf файла");
			}
			case "html": {
				return HTMLType.getData(filePath);
			}
			case "xml": {
				return XMLType.getData(filePath);
			}
			default: throw new ParseException("Неизвестный тип файла");
		}
	}
	
	private static String getText(Path path) throws ParseException {
		AutoDetectParser parser = new AutoDetectParser(tikaConfig);
		ContentHandler handler = new BodyContentHandler(-1);
		try (TikaInputStream stream = TikaInputStream.get(Files.newInputStream(path))) {
			parser.parse(stream, handler, new Metadata(), new ParseContext());
			return handler.toString();
		} catch (IOException | SAXException | TikaException e) {
			throw new ParseException("Невозможно прочитать файл");
		}
	}
}
