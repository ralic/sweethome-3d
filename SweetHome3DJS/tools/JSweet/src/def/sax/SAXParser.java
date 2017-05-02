package def.sax;

class DefaultHandler {

}

interface Attributes {
  int getIndex(String qName);

  int getIndex(String uri, String localName);

  int getLength();

  String getLocalName(int index);

  String getQName(int index);

  String getType(int index);

  String getType(String qName);

  String getType(String uri, String localName);

  String getURI(int index);

  String getValue(int index);

  String getValue(String qName);

  String getValue(String uri, String localName);

}

@SuppressWarnings("serial")
class SAXException extends Error {

  public SAXException(String message) {
  }

  public SAXException(String message, Error cause) {
  }

}
