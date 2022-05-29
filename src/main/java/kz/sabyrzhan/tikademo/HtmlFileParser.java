package kz.sabyrzhan.tikademo;

import lombok.ToString;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.html.HtmlMapper;
import org.apache.tika.parser.html.HtmlParser;
import org.apache.tika.parser.html.IdentityHtmlMapper;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class HtmlFileParser {
    public static void main(String[] args) throws Exception {
        ParseContext context = new ParseContext();
        context.set(HtmlMapper.class, new IdentityHtmlMapper());

        Parser parser = new HtmlParser();
        Metadata metadata = new Metadata();
        metadata.set(Metadata.CONTENT_TYPE, "text/html");

        ParserHandler contentHandler = new ParserHandler();
        try(InputStream inputStream = HtmlFileParser.class.getClassLoader().getResourceAsStream("html_page.html")) {
            parser.parse(inputStream, contentHandler, metadata, context);
        }

        contentHandler.items.stream().forEach(System.out::println);
    }

    public static class ParserHandler extends DefaultHandler {
        @ToString
        public class ProductItem {
            private String title;
            private String price;
            private String image;
        }
        public record Element(String name, Set<String> cssClasses, Map<String, String> attributes) {
            public boolean cssClassExists(String cssClass) {
                return cssClasses.contains(cssClass);
            }
        }

        private List<ProductItem> items = new ArrayList<>();
        private Stack<Element> elements = new Stack<>();
        private Element currentElem;
        private boolean isDivSkipped;

        private ProductItem tmp;

        @Override
        public void startElement(String uri, String localName, String name, Attributes atts) throws SAXException {
            String currentElemClass = Optional.ofNullable(atts.getValue("class")).orElse("");

            Map<String, String> attribs = new HashMap<>();
            Optional.ofNullable(atts.getValue("src")).ifPresent(src -> attribs.put("src", src));

            Set<String> cssClasses = Arrays.stream(currentElemClass.split(" ")).collect(Collectors.toSet());

            currentElem = new Element(localName, cssClasses, attribs);
            elements.push(currentElem);

            if (currentElem.name.equals("div") && currentElem.cssClassExists("ProductCardV")) {
                if (currentElem.cssClasses.contains("--loading")) {
                    isDivSkipped = true;
                } else {
                    isDivSkipped = false;
                    tmp = new ProductItem();
                }
            }
        }

        @Override
        public void endElement(String uri, String localName, String name) throws SAXException {
            if (elements.isEmpty() || isDivSkipped) {
                return;
            }

            Element lastElem = elements.pop();
            if (lastElem.name.equals("div") && lastElem.cssClassExists("ProductCardV")) {
                items.add(tmp);
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (isDivSkipped) {
                return;
            }
            String str = new String(ch, start, length).strip();
            String name = currentElem.name();
            Set<String> tokens = currentElem.cssClasses;
            if (name.equals("p") && tokens.contains("ProductCardV__Title") && !str.isEmpty()) {
                tmp.title = str;
            }

            if (name.equals("img") && tokens.contains("LazyImage__Source")) {
                String src = Optional.ofNullable(currentElem.attributes.get("src")).orElse("");
                if (!src.isEmpty()) {
                    tmp.image = src;
                }
            }

            if (name.equals("p") && tokens.contains("ProductCardV__Price") && !str.isEmpty()) {
                tmp.price = str;
            }
        }
    }
}
