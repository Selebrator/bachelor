package de.lukaspanneke.bachelor.parser;

import de.lukaspanneke.bachelor.mcc.MccReachabilityCardinalityProperty;
import de.lukaspanneke.bachelor.mcc.MccReachabilityCardinalityPropertySet;
import de.lukaspanneke.bachelor.reachability.logic.BinaryLogicOperator;
import de.lukaspanneke.bachelor.reachability.logic.CalculationOperator;
import de.lukaspanneke.bachelor.reachability.logic.ComparisonOperator;
import de.lukaspanneke.bachelor.reachability.logic.generic.Formula;
import de.lukaspanneke.bachelor.reachability.logic.generic.FormulaBuilder;
import de.lukaspanneke.bachelor.reachability.logic.generic.expression.ArithmeticExpression;
import de.lukaspanneke.bachelor.reachability.logic.generic.formula.StateFormula;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import uniol.apt.io.parser.ParseException;
import uniol.apt.io.parser.impl.AbstractParser;
import uniol.apt.io.parser.impl.ParseRuntimeException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class MccXmlReachabilityCardinalityParser<N, M, P, T> extends AbstractParser<MccReachabilityCardinalityPropertySet<N, M, P, T>> {

	private final FormulaBuilder<N, M, P, T> formulaBuilder;

	public MccXmlReachabilityCardinalityParser(FormulaBuilder<N, M, P, T> formulaBuilder) {
		this.formulaBuilder = formulaBuilder;
	}

	@Override
	public String getFormat() {
		return "xml";
	}

	@Override
	public List<String> getFileExtensions() {
		return List.of("xml");
	}

	@Override
	public MccReachabilityCardinalityPropertySet<N, M, P, T> parse(InputStream inputStream) throws ParseException, IOException {
		return new Parser().parse(inputStream);
	}

	private class Parser {

		private MccReachabilityCardinalityPropertySet<N, M, P, T> parse(InputStream is) throws ParseException, IOException {
			Document doc = getDocument(is);
			Element root = getRoot(doc);

			List<Element> properties = getChildElements(root, "property");
			return new MccReachabilityCardinalityPropertySet<>(index -> {
				try {
					return parseProperty(properties.get(index));
				} catch (ParseException e) {
					throw new ParseRuntimeException(e);
				}
			}, properties.size());
		}

		private MccReachabilityCardinalityProperty<N, M, P, T> parseProperty(Element propertyElement) throws ParseException {
			String id;
			{
				Element idElement = getOptionalChildElement(propertyElement, "id");
				if (idElement != null) {
					id = getText(idElement);
				} else {
					id = null;
				}
			}
			String description;
			{
				Element descriptionElement = getOptionalChildElement(propertyElement, "description");
				if (descriptionElement != null) {
					description = getText(descriptionElement);
				} else {
					description = null;
				}
			}

			Formula<N, M, P, T> formula;
			try {
				Element formulaElement = getChildElement(propertyElement, "formula");
				Element elem;
				if ((elem = getOptionalChildElement(formulaElement, "all-paths")) != null) {
					formula = Formula.allGlobally(parseStateFormula(getChildElement(getChildElement(elem, "globally"))));
				} else if ((elem = getOptionalChildElement(formulaElement, "exists-path")) != null) {
					formula = Formula.existsFinally(parseStateFormula(getChildElement(getChildElement(elem, "finally"))));
				} else {
					throw new ParseException("Formulas must begin with 'exists finally' or 'all globally'");
				}

			} catch (Exception e) {
				if (id != null) {
					throw new ParseException("Error while parsing " + id, e);
				} else {
					throw e;
				}
			}
			return new MccReachabilityCardinalityProperty<>(id, description, formula);
		}

		private StateFormula<N, M, P, T> parseComparison(Element comparisonElement, ComparisonOperator operator) throws ParseException {
			List<Element> childElements = getChildElements(comparisonElement);
			if (childElements.size() != 2) {
				throw new ParseException("Must compare 2 expressions");
			}
			return formulaBuilder.compare(parseArithmeticExpression(childElements.get(0)), operator, parseArithmeticExpression(childElements.get(1)));
		}

		private ArithmeticExpression<N, M, P, T> parseArithmeticExpression(Element element) throws ParseException {
			return switch (element.getTagName()) {
				case "integer-constant" -> formulaBuilder.constant(Long.parseLong(getText(element)));
				case "tokens-count" -> {
					Iterator<Element> places = getChildElements(element, "place").iterator();
					if (!places.hasNext()) {
						throw new ParseException("Expected as least one place in <tokens-count>, but found 0");
					}
					ArithmeticExpression<N, M, P, T> expr = formulaBuilder.place(getText(places.next()));

					while (places.hasNext()) {
						expr = formulaBuilder.calculate(expr, CalculationOperator.PLUS, formulaBuilder.place(getText(places.next())));
					}
					yield expr;
				}
				default -> throw new ParseException("Unknown arithmetic expression " + element);
			};
		}

		private StateFormula<N, M, P, T> parseStateFormula(Element element) throws ParseException {
			return switch (element.getTagName()) {
				case "negation" -> formulaBuilder.negate(parseStateFormula(getChildElement(element)));
				case "conjunction" -> parseBooleanFunction(element, BinaryLogicOperator.AND);
				case "disjunction" -> parseBooleanFunction(element, BinaryLogicOperator.OR);
				case "integer-le" -> parseComparison(element, ComparisonOperator.LESS_EQUALS);
				default -> throw new UnsupportedOperationException(element.toString()); // TODO
			};
		}

		private StateFormula<N, M, P, T> parseBooleanFunction(Element binaryOperatorElement, BinaryLogicOperator operator) throws ParseException {
			List<Element> childElements = getChildElements(binaryOperatorElement);
			if (childElements.size() == 0) {
				throw new ParseException("Conjunction of 0 elements is not allowed");
			}
			StateFormula<N, M, P, T> ret = parseStateFormula(childElements.get(0));
			for (int i = 1; i < childElements.size(); i++) {
				ret = formulaBuilder.compose(ret, operator, parseStateFormula(childElements.get(i)));
			}
			return ret;
		}

		/**
		 * Returns the root {@code property-set} element.
		 */
		private Element getRoot(Document doc) throws ParseException {
			Element root = doc.getDocumentElement();
			if (!root.getNodeName().equals("property-set")) {
				throw new ParseException("Root element isn't <property-set>");
			}
			return root;
		}

		/**
		 * Parses an xml file into a DOM model.
		 */
		private Document getDocument(InputStream is) throws ParseException, IOException {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder;
			try {
				builder = factory.newDocumentBuilder();
			} catch (ParserConfigurationException e) {
				throw new ParseException("Internal error while parsing the document", e);
			}
			builder.setErrorHandler(new ErrorHandler() {
				@Override
				public void warning(SAXParseException e) {
					// Silently ignore warnings
				}

				@Override
				public void error(SAXParseException e) throws SAXException {
					throw e;
				}

				@Override
				public void fatalError(SAXParseException e) throws SAXException {
					throw e;
				}
			});
			try {
				return builder.parse(is);
			} catch (SAXException e) {
				throw new ParseException("Could not parse PNML XML file", e);
			}
		}

		/**
		 * Returns a list of all child elements.
		 *
		 * @param parent parent element
		 * @return list of child elements
		 */
		private List<Element> getChildElements(Element parent) {
			List<Element> elements = new ArrayList<>();
			NodeList children = parent.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child.getNodeType() == Node.ELEMENT_NODE) {
					elements.add((Element) child);
				}
			}
			return elements;
		}

		/**
		 * Returns a list of all child elements with the given tag name.
		 *
		 * @param parent parent element
		 * @param tagName child tag name
		 * @return list of child elements
		 */
		private List<Element> getChildElements(Element parent, String tagName) {
			List<Element> elements = new ArrayList<>();
			NodeList children = parent.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals(tagName)) {
					elements.add((Element) child);
				}
			}
			return elements;
		}

		/**
		 * Returns a single child element.
		 *
		 * @param parent parent element
		 * @return the child element
		 * @throws ParseException thrown when not exactly one child if found
		 */
		private Element getChildElement(Element parent) throws ParseException {
			List<Element> elements = getChildElements(parent);
			if (elements.size() == 1) {
				return elements.get(0);
			} else {
				throw new ParseException(
						String.format("Expected single child of parent <%s> but found %d",
								parent.getTagName(), elements.size()));
			}
		}

		/**
		 * Returns a single child element with the given name.
		 *
		 * @param parent parent element
		 * @param tagName child tag name
		 * @return the child element
		 * @throws ParseException thrown when not exactly one child with the
		 * given name is found
		 */
		private Element getChildElement(Element parent, String tagName) throws ParseException {
			List<Element> elements = getChildElements(parent, tagName);
			if (elements.size() == 1) {
				return elements.get(0);
			} else {
				throw new ParseException(
						String.format("Expected single child <%s> of parent <%s> but found %d",
								tagName, parent.getTagName(), elements.size()));
			}
		}

		/**
		 * Returns a single child element with the given name.
		 *
		 * @param parent parent element
		 * @param tagName child tag name
		 * @return the child element
		 */
		private Element getOptionalChildElement(Element parent, String tagName) {
			List<Element> elements = getChildElements(parent, tagName);
			if (elements.size() == 1) {
				return elements.get(0);
			} else {
				return null;
			}
		}

		/**
		 * Returns an attribute's value.
		 *
		 * @param elem element of which the attribute is to be returned
		 * @param attrName attribute name
		 * @return attribute value
		 * @throws ParseException thrown when the attribute does not exist
		 */
		private String getAttribute(Element elem, String attrName) throws ParseException {
			String attr = getOptionalAttribute(elem, attrName);
			if (attr == null) {
				throw new ParseException(
						"Element <" + elem.getTagName() + "> does not have attribute " + attrName);
			}
			return attr;
		}


		/**
		 * Returns an attribute's value.
		 *
		 * @param elem element of which the attribute is to be returned
		 * @param attrName attribute name
		 * @return attribute value
		 */
		private String getOptionalAttribute(Element elem, String attrName) {
			if (!elem.hasAttribute(attrName)) {
				return null;
			}
			return elem.getAttribute(attrName);
		}

		/**
		 * Returns the text contents of an element.
		 *
		 * @param element parent element
		 * @return text enclosed by parent element
		 * @throws ParseException thrown when there are other elements inside
		 * the parent element
		 */
		private String getText(Element element) throws ParseException {
			Node child = element.getFirstChild();
			if (child == null || child.getNextSibling() != null)
				throw new ParseException("Trying to get text inside of <" + element.getTagName()
						+ ">, but this element has multiple children");
			if (!(child instanceof Text))
				throw new ParseException("Trying to get text inside of <" + element.getTagName()
						+ ">, but child isn't text");
			return child.getNodeValue();
		}
	}
}
