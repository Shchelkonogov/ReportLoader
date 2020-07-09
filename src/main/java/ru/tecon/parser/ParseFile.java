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
import ru.tecon.parser.model.ReportData;
import ru.tecon.parser.types.*;
import ru.tecon.parser.types.html.HTMLType;
import ru.tecon.parser.types.xml.XMLType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class ParseFile {
	
	private static TikaConfig tikaConfig = TikaConfig.getDefaultConfig();

	private ParseFile() {
	}

	public static ReportData parseFile(String filePath) throws ParseException {
		String text;
		switch (filePath.substring(filePath.lastIndexOf(".") + 1).toLowerCase()) {
			case "pdf": {
				text = getText(new File(filePath));
				if (text == null) {
					throw new ParseException("can't get text from file");
				}
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
				throw new ParseException("don't have pdf parser");
			}
			case "html": {
				return HTMLType.getData(filePath);
			}
			case "xml": {
				return XMLType.getData(filePath);
			}
			default: throw new ParseException("don't have file parser");
		}
	}
	
	private static String getText(File file) {
		try {
			Metadata metadata = new Metadata();
			AutoDetectParser parser = new AutoDetectParser(tikaConfig);
			ContentHandler handler = new BodyContentHandler(-1);
			TikaInputStream stream = TikaInputStream.get(new FileInputStream(file));
			parser.parse(stream, handler, metadata, new ParseContext());
			return handler.toString();
		} catch (IOException | SAXException | TikaException e) {
			return null;
		}
	}
}
