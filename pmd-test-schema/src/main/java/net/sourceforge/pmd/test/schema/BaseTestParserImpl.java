/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.test.schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.sourceforge.pmd.Rule;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.properties.PropertyDescriptor;
import net.sourceforge.pmd.properties.PropertySource;
import net.sourceforge.pmd.test.schema.TestSchemaParser.PmdXmlReporter;

import com.github.oowekyala.ooxml.DomUtils;

/**
 * @author Clément Fournier
 */
class BaseTestParserImpl {

    static class ParserV1 extends BaseTestParserImpl {

    }

    public TestCollection parseDocument(Rule rule, Document doc, PmdXmlReporter err) {
        Element root = doc.getDocumentElement();

        Map<String, Element> codeFragments = parseCodeFragments(err, root);

        Set<String> usedFragments = new HashSet<>();
        NodeList testCodes = root.getElementsByTagName("test-code");
        TestCollection result = new TestCollection();
        for (int i = 0; i < testCodes.getLength(); i++) {
            TestDescriptor descriptor = new TestDescriptor(i, rule.deepCopy());

            PmdXmlReporter errScope = err.newScope();
            parseSingleTest((Element) testCodes.item(i), descriptor, codeFragments, usedFragments, errScope);
            if (!errScope.hasError()) {
                result.addTest(descriptor);
            }
        }

        codeFragments.keySet().removeAll(usedFragments);
        codeFragments.forEach((id, node) -> err.at(node).warn("Unused code fragment"));

        return result;
    }

    private Map<String, Element> parseCodeFragments(PmdXmlReporter err, Element root) {
        Map<String, Element> codeFragments = new HashMap<>();

        for (Node node : DomUtils.asList(root.getElementsByTagName("code-fragment"))) {
            Attr id = getRequiredAttribute("id", (Element) node, err);
            if (id == null) {
                continue;
            }

            Element prev = codeFragments.put(id.getValue(), (Element) node);
            if (prev != null) {
                err.at(prev).error("Fragment with duplicate id ''{0}'' is ignored", id.getValue());
            }
        }
        return codeFragments;
    }

    private void parseSingleTest(Element testCode, TestDescriptor descriptor, Map<String, Element> fragments, Set<String> usedFragments, PmdXmlReporter err) {
        {
            String description = getSingleChildText(testCode, "description", true, err);
            if (description == null) {
                return;
            }
            descriptor.setDescription(description);
        }

        parseBoolAttribute(testCode, "reinitializeRule", true, err, "Attribute 'reinitializeRule' is deprecated and ignored, assumed true");
        parseBoolAttribute(testCode, "useAuxClasspath", true, err, "Attribute 'useAuxClasspath' is deprecated and ignored, assumed true");

        boolean ignored = parseBoolAttribute(testCode, "ignored", false, err, null)
                          && !parseBoolAttribute(testCode, "regressionTest", true, err, "Attribute ''regressionTest'' is deprecated, use ''ignored'' with inverted value");

        descriptor.setIgnored(ignored);

        Properties properties = parseRuleProperties(testCode, descriptor.getRule(), err);
        descriptor.getProperties().putAll(properties);

        parseExpectedProblems(testCode, descriptor, err);

        String code = getTestCode(testCode, fragments, err);
        if (code == null) {
            return;
        }
        descriptor.setCode(code);


        LanguageVersion lversion = parseLanguageVersion(testCode, err);
        descriptor.setLanguageVersion(lversion);
    }

    private void parseExpectedProblems(Element testCode, TestDescriptor descriptor, PmdXmlReporter err) {
        Node expectedProblemsNode = getSingleChild(testCode, "expected-problems", true, err);
        if (expectedProblemsNode == null) {
            return;
        }
        int expectedProblems = Integer.parseInt(parseTextNode(expectedProblemsNode));

        List<String> expectedMessages = null;
        {
            Element messagesNode = getSingleChild(testCode, "expected-messages", false, err);
            if (messagesNode != null) {
                expectedMessages = new ArrayList<>();
                List<Node> messageNodes = DomUtils.asList(messagesNode.getElementsByTagName("message"));
                if (messageNodes.size() != expectedProblems) {
                    err.at(expectedProblemsNode).error("Number of ''expected-messages'' ({0}) does not match", messageNodes.size());
                    return;
                }

                for (Node message : messageNodes) {
                    expectedMessages.add(parseTextNode(message));
                }
            }
        }

        List<Integer> expectedLineNumbers = null;
        {
            Element lineNumbers = getSingleChild(testCode, "expected-linenumbers", false, err);
            if (lineNumbers != null) {
                expectedLineNumbers = new ArrayList<>();
                String[] linenos = parseTextNode(lineNumbers).split(",");
                if (linenos.length != expectedProblems) {
                    err.at(expectedProblemsNode).error("Number of ''expected-linenumbers'' ({0}) does not match", linenos.length);
                    return;
                }
                for (String num : linenos) {
                    expectedLineNumbers.add(Integer.valueOf(num.trim()));
                }
            }
        }

        descriptor.recordExpectedViolations(
            expectedProblems,
            expectedLineNumbers,
            expectedMessages
        );

    }

    private String getTestCode(Element testCode, Map<String, Element> fragments, PmdXmlReporter err) {
        String code = getSingleChildText(testCode, "code", false, err);
        if (code == null) {
            // Should have a coderef
            NodeList coderefs = testCode.getElementsByTagName("code-ref");
            if (coderefs.getLength() == 0) {
                throw new RuntimeException(
                    "Required tag is missing from the test-xml. Supply either a code or a code-ref tag");
            }
            Element coderef = (Element) coderefs.item(0);
            Attr id = getRequiredAttribute("id", coderef, err);
            if (id == null) {
                return null;
            }
            Element fragment = fragments.get(id.getValue());
            if (fragment == null) {
                err.at(id).error("Unknown id, known IDs are {0}", fragments.keySet());
                return null;
            }
            code = parseTextNodeNoTrim(fragment);
        }
        return code;
    }

    private LanguageVersion parseLanguageVersion(Element testCode, PmdXmlReporter err) {
        Node sourceTypeNode = getSingleChild(testCode, "source-type", false, err);
        if (sourceTypeNode == null) {
            return null;
        }
        String languageVersionString = parseTextNode(sourceTypeNode);
        LanguageVersion languageVersion = LanguageRegistry.findLanguageVersionByTerseName(languageVersionString);
        if (languageVersion != null) {
            return languageVersion;
        }

        err.at(sourceTypeNode).error("Unknown language version ''{0}''", languageVersionString);
        return null;
    }

    private Properties parseRuleProperties(Element testCode, PropertySource knownProps, PmdXmlReporter err) {
        Properties properties = new Properties();
        for (Node ruleProperty : DomUtils.asList(testCode.getElementsByTagName("rule-property"))) {
            Node nameAttr = getRequiredAttribute("name", (Element) ruleProperty, err);
            if (nameAttr == null) {
                continue;
            }
            String propertyName = nameAttr.getNodeValue();
            if (knownProps.getPropertyDescriptor(propertyName) == null) {
                Stream<String> knownNames = knownProps.getPropertyDescriptors().stream().map(PropertyDescriptor::name);
                err.at(nameAttr).error("Unknown property, known property names are {0}", knownNames);
                continue;
            }
            properties.setProperty(propertyName, parseTextNode(ruleProperty));
        }
        return properties;
    }

    private Attr getRequiredAttribute(String name, Element ruleProperty, PmdXmlReporter err) {
        Attr nameAttr = (Attr) ruleProperty.getAttributes().getNamedItem(name);
        if (nameAttr == null) {
            err.at(ruleProperty).error("Missing ''{0}'' attribute", name);
            return null;
        }
        return nameAttr;
    }

    private boolean parseBoolAttribute(Element testCode, String attrName, boolean defaultValue, PmdXmlReporter err, String deprecationMessage) {
        Attr attrNode = testCode.getAttributeNode(attrName);
        if (attrNode != null) {
            if (deprecationMessage != null) {
                err.at(attrNode).warn(deprecationMessage);
            }
            return Boolean.parseBoolean(attrNode.getNodeValue());
        }
        return defaultValue;
    }


    private String getSingleChildText(Element parentElm, String nodeName, boolean required, PmdXmlReporter err) {
        Node node = getSingleChild(parentElm, nodeName, required, err);
        if (node == null) {
            return null;
        }
        return parseTextNode(node);
    }

    private Element getSingleChild(Element parentElm, String nodeName, boolean required, PmdXmlReporter err) {
        NodeList nodes = parentElm.getElementsByTagName(nodeName);
        if (nodes.getLength() == 0) {
            if (required) {
                err.at(parentElm).error("Required child ''{0}'' is missing", nodeName);
            }
            return null;
        } else if (nodes.getLength() > 1) {
            err.at(nodes.item(1)).error("Duplicate tag ''{0}'' is ignored", nodeName);
        }
        return (Element) nodes.item(0);
    }

    private static String parseTextNode(Node exampleNode) {
        return parseTextNodeNoTrim(exampleNode).trim();
    }

    private static String parseTextNodeNoTrim(Node exampleNode) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < exampleNode.getChildNodes().getLength(); i++) {
            Node node = exampleNode.getChildNodes().item(i);
            if (node.getNodeType() == Node.CDATA_SECTION_NODE || node.getNodeType() == Node.TEXT_NODE) {
                buffer.append(node.getNodeValue());
            }
        }
        return buffer.toString();
    }


}
