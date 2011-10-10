/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hp.hpl.jena.sparql.resultset;

import static com.hp.hpl.jena.sparql.resultset.JSONResults.* ;

import java.io.OutputStream ;
import java.util.HashMap ;
import java.util.Iterator ;
import java.util.Map ;

import org.openjena.atlas.io.IndentedWriter ;
import org.openjena.atlas.json.io.JSWriter ;

import com.hp.hpl.jena.query.ARQ ;
import com.hp.hpl.jena.query.QuerySolution ;
import com.hp.hpl.jena.query.ResultSet ;
import com.hp.hpl.jena.rdf.model.Literal ;
import com.hp.hpl.jena.rdf.model.RDFNode ;
import com.hp.hpl.jena.rdf.model.Resource ;
import org.openjena.atlas.logging.Log ;

/**
 * A JSON writer for SPARQL Result Sets.  Uses Jena Atlas JSON support. 
 * 
 * Format: <a href="http://www.w3.org/TR/sparql11-results-json/">SPARQL 1.1 Query Results JSON Format</a> 
 */ 

public class JSONOutputResultSet implements ResultSetProcessor
{
    static boolean multiLineValues = false ;
    static boolean multiLineVarNames = false ;
    
    private boolean outputGraphBNodeLabels = false ;
    private IndentedWriter out ;
    private int bNodeCounter = 0 ;
    private Map<Resource, String> bNodeMap = new HashMap<Resource, String>() ;
    
    JSONOutputResultSet(OutputStream outStream)
    { this(new IndentedWriter(outStream)) ; }
    
    JSONOutputResultSet(IndentedWriter indentedOut)
    {   out = indentedOut ;
        outputGraphBNodeLabels = ARQ.isTrue(ARQ.outputGraphBNodeLabels) ;
    }
    
    public void start(ResultSet rs)
    {
        out.println("{") ;
        out.incIndent() ;
        doHead(rs) ;
        out.println(quoteName(dfResults)+": {") ;
        out.incIndent() ;
        out.println(quoteName(dfBindings)+": [") ;
        out.incIndent() ;
        firstSolution = true ;
    }

    public void finish(ResultSet rs)
    {
        // Close last binding.
        out.println() ;
        
        out.decIndent() ;       // bindings
        out.println("]") ;
        out.decIndent() ;
        out.println("}") ;      // results
        out.decIndent() ;
        out.println("}") ;      // top level {}
        out.flush() ;
    }

    private void doHead(ResultSet rs)
    {
        out.println(quoteName(dfHead)+": {") ;
        out.incIndent() ;
        doLink(rs) ;
        doVars(rs) ;
        out.decIndent() ;
        out.println("} ,") ;
    }
    
    private void doLink(ResultSet rs)
    {
        // ---- link
        //out.println("\"link\": []") ;
    }
    
    private void doVars(ResultSet rs)
    {
        // On one line.
        out.print(quoteName(dfVars)+": [ ") ;
        if ( multiLineVarNames ) out.println() ;
        out.incIndent() ;
        for (Iterator<String> iter = rs.getResultVars().iterator() ; iter.hasNext() ; )
        {
            String varname = iter.next() ;
            out.print("\""+varname+"\"") ;
            if ( multiLineVarNames ) out.println() ;
            if ( iter.hasNext() )
                out.print(" , ") ;
        }
        out.println(" ]") ;
        out.decIndent() ;
    }

    boolean firstSolution = true ;
    boolean firstBindingInSolution = true ;
    
    // NB assumes are on end of previous line.
    public void start(QuerySolution qs)
    {
        if ( ! firstSolution )
            out.println(" ,") ;
        firstSolution = false ;
        out.println("{") ;
        out.incIndent() ;
        firstBindingInSolution = true ;
    }

    public void finish(QuerySolution qs)
    {
        out.println() ;     // Finish last binding
        out.decIndent() ;
        out.print("}") ;    // NB No newline
    }

    public void binding(String varName, RDFNode value)
    {
        if ( value == null )
            return ;
        
        if ( !firstBindingInSolution )
            out.println(" ,") ;
        firstBindingInSolution = false ;

        // Do not use quoteName - varName may not be JSON-safe as a bare name.
        out.print(quote(varName)+": { ") ;
        if ( multiLineValues ) out.println() ;
        
        out.incIndent() ;
        // Old, explicit unbound
//        if ( value == null )
//            printUnbound() ;
//        else
      	if ( value.isLiteral() )
            printLiteral((Literal)value) ;
        else if ( value.isResource() )
            printResource((Resource)value) ;
        else 
            Log.warn(this, "Unknown RDFNode type in result set: "+value.getClass()) ;
        out.decIndent() ;
        
        if ( !multiLineValues ) out.print(" ") ; 
        out.print("}") ;        // NB No newline
    }
    
    private void printUnbound()
    {
        out.print(quoteName(dfType)+ ": "+quote(dfUnbound)+" , ") ;
        if ( multiLineValues ) out.println() ;
        out.print(quoteName(dfValue)+": null") ;
        if ( multiLineValues ) out.println() ;
    }

    private void printLiteral(Literal literal)
    {
        String datatype = literal.getDatatypeURI() ;
        String lang = literal.getLanguage() ;
        
        if ( datatype != null )
        {
            out.print(quoteName(dfDatatype)+": "+quote(datatype)+" , ") ;
            if ( multiLineValues ) out.println() ;
            
            out.print(quoteName(dfType)+": "+quote(dfTypedLiteral)+" , ") ;
            if ( multiLineValues ) out.println() ;
        }
        else
        {
            out.print(quoteName(dfType)+": "+quote(dfLiteral)+" , ") ;
            if ( multiLineValues ) out.println() ;
            
            if ( lang != null && !lang.equals("") )
            {
                out.print(quoteName(dfLang)+": "+quote(lang)+" , ") ;
                if ( multiLineValues ) out.println() ;
            }
        }
            
        out.print(quoteName(dfValue)+": "+quote(literal.getLexicalForm())) ;
        if ( multiLineValues ) out.println() ;
    }

    private void printResource(Resource resource)
    {
        if ( resource.isAnon() )
        {
            String label ; 
            if ( outputGraphBNodeLabels )
                label = resource.getId().getLabelString() ;
            else
            {
                if ( ! bNodeMap.containsKey(resource))
                    bNodeMap.put(resource, "b"+(bNodeCounter++)) ;
                label = bNodeMap.get(resource) ;
            }
            
            out.print(quoteName(dfType)+": "+quote(dfBNode)+" , ") ;
            if ( multiLineValues ) out.println() ;
            
            out.print(quoteName(dfValue)+": "+quote(label)) ;
            
            if ( multiLineValues ) out.println() ;
        }
        else
        {
            out.print(quoteName(dfType)+": "+quote(dfURI)+" , ") ;
            if ( multiLineValues ) out.println() ;
            out.print(quoteName(dfValue)+": "+quote(resource.getURI())) ;
            if ( multiLineValues ) out.println() ;
            return ;
        }
    }
    
    private static String quote(String string)
    {
        return JSWriter.outputQuotedString(string) ;
    }
    
    // Quote a name (known to be JSON-safe)
    // Never the RHS of a member entry (for example "false")
    // Some (the Java JSON code for one) JSON parsers accept an unquoted
    // string as a name of a name/value pair.
    
    private static String quoteName(String string)
    {
        // Safest to quote anyway.
        return quote(string) ;
        
        // Assumes only called with safe names
        //return string ;
        
        // Better would be:
        // starts a-z, constains a-z,0-9, not a keyword(true, false, null)
//        if ( string.contains(something not in a-z0-9)
//        and         
//        //return "\""+string+"\"" ;
//        return JSONObject.quote(string) ;
    }

}
