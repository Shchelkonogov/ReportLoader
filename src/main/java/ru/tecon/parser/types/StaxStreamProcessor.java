package ru.tecon.parser.types;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import java.io.InputStream;
import java.util.List;

public class StaxStreamProcessor implements AutoCloseable {

    private static final XMLInputFactory FACTORY = XMLInputFactory.newInstance();

    private final XMLStreamReader reader;

    public StaxStreamProcessor(InputStream is) throws XMLStreamException {
        reader = FACTORY.createXMLStreamReader(is);
    }

    private boolean startRead = false;

    public boolean getElement(String element) throws XMLStreamException {
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLEvent.START_ELEMENT && element.equals(reader.getLocalName())) {
                return true;
            }
        }
        return false;
    }

    public boolean getElement(List<String> element) throws XMLStreamException {
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLEvent.START_ELEMENT && element.contains(reader.getLocalName())) {
                return true;
            }
        }
        return false;
    }

    public boolean checkElement(String element1, String element2) throws XMLStreamException {
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLEvent.START_ELEMENT && element1.equals(reader.getLocalName())) {
                return true;
            }

            if (event == XMLEvent.END_ELEMENT && element2.equals(reader.getLocalName())) {
                return false;
            }
        }
        return false;
    }

    public String checkElement(List<String> element1, String element2) throws XMLStreamException {
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLEvent.START_ELEMENT && element1.contains(reader.getLocalName())) {
                return reader.getLocalName();
            }

            if (event == XMLEvent.END_ELEMENT && element2.equals(reader.getLocalName())) {
                return null;
            }
        }
        return null;
    }

    public boolean startElement(String element, String parent) throws XMLStreamException {
        while (reader.hasNext()) {
            int event = reader.next();
            if (parent != null && event == XMLEvent.END_ELEMENT &&
                    parent.equals(reader.getLocalName())) {
                startRead = false;
                return false;
            }

            if (!startRead && parent != null && event == XMLEvent.START_ELEMENT &&
                    parent.equals(reader.getLocalName())) {
                startRead = true;
            }

            if (event == XMLEvent.START_ELEMENT &&
                    element.equals(reader.getLocalName()) && startRead) {
                return true;
            }
        }
        startRead = false;
        return false;
    }


    public String getText() throws XMLStreamException {
        return reader.getElementText();
    }

    public String getLocalName() {
        return reader.getLocalName();
    }

    public String checkElement(String element2) throws XMLStreamException {
        StringBuilder result = new StringBuilder();
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLEvent.START_ELEMENT) {
                result.append(reader.getLocalName()).append("=").append(reader.getElementText()).append("|");
            }

            if (event == XMLEvent.END_ELEMENT && element2.equals(reader.getLocalName())) {
                break;
            }
        }
        return result.toString();
    }

    public String getValue() throws XMLStreamException {
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLEvent.START_ELEMENT && reader.getLocalName().equals("VALUE")) {
                return reader.getElementText();
            }
        }
        return null;
    }

    @Override
    public void close() {
        if (reader != null) {
            try {
                reader.close();
            } catch (XMLStreamException ignore) {
            }
        }
    }
}
