package org.apache.jena.query.text.assembler;

import java.util.Hashtable;

import org.apache.jena.assembler.Assembler;
import org.apache.jena.assembler.Mode;
import org.apache.jena.assembler.assemblers.AssemblerBase;
import org.apache.jena.query.text.TextIndexException;
import org.apache.jena.query.text.analyzer.Util;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.lucene.analysis.Analyzer;

public class DefinedAnalyzerAssembler extends AssemblerBase {
    
    private static Hashtable<Resource, Analyzer> analyzers = new Hashtable<>();
    
    public static void addAnalyzer(Resource key, Analyzer analyzer) {
        analyzers.put(key, analyzer);
    }
    
    public static boolean addAnalyzers(Assembler a, Resource list) {
        Resource current = list;
        boolean isMultilingualSupport = false;
        
        while (current != null && ! current.equals(RDF.nil)){
            Statement firstStmt = current.getProperty(RDF.first);
            if (firstStmt == null) {
                throw new TextIndexException("parameter list not well formed: " + current);
            }
            
            RDFNode first = firstStmt.getObject();
            if (! first.isResource()) {
                throw new TextIndexException("parameter specification must be an anon resource : " + first);
            }

            // process the current list element to add an analyzer 
            Resource adding = (Resource) first;
            if (adding.hasProperty(TextVocab.pAnalyzer)) {
                Statement analyzerStmt = adding.getProperty(TextVocab.pAnalyzer);
                RDFNode analyzerNode = analyzerStmt.getObject();
                if (!analyzerNode.isResource()) {
                    throw new TextIndexException("addAnalyzers text:analyzer must be an analyzer spec resource: " + analyzerNode);
                }
                
                Analyzer analyzer = (Analyzer) a.open((Resource) analyzerNode);
                
                if (adding.hasProperty(TextVocab.pAddLang)) {
                    Statement langStmt = adding.getProperty(TextVocab.pAddLang);
                    String langCode = langStmt.getString();
                    Util.addAnalyzer(langCode, analyzer);
                    isMultilingualSupport = true;
                }
                
                if (adding.hasProperty(TextVocab.pDefAnalyzer)) {
                    Statement defStmt = adding.getProperty(TextVocab.pDefAnalyzer);
                    Resource id = defStmt.getResource();
                    
                    if (id.getURI() != null) {
                        DefinedAnalyzerAssembler.addAnalyzer(id, analyzer);
                    } else {
                        throw new TextIndexException("addAnalyzers text:defineAnalyzer property must be a non-blank resource: " + adding);
                    }
                }
            } else {
                throw new TextIndexException("text:analyzer property is required when adding an analyzer: " + adding);
            }
            
            Statement restStmt = current.getProperty(RDF.rest);
            if (restStmt == null) {
                throw new TextIndexException("parameter list not terminated by rdf:nil");
            }
            
            RDFNode rest = restStmt.getObject();
            if (! rest.isResource()) {
                throw new TextIndexException("parameter list node is not a resource : " + rest);
            }
            
            current = (Resource) rest;
        }
        
        return isMultilingualSupport;
    }
   
    @Override
    public Object open(Assembler a, Resource root, Mode mode) {
        
        if (root.hasProperty(TextVocab.pUseAnalyzer)) {
            Statement useStmt = root.getProperty(TextVocab.pUseAnalyzer);
            Resource key = useStmt.getResource();
            
            return analyzers.get(key);
        }
        
        return null;
    }

}
