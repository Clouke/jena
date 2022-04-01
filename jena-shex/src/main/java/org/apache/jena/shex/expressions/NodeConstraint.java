/*
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

package org.apache.jena.shex.expressions;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.graph.Node;
import org.apache.jena.shex.sys.ReportItem;
import org.apache.jena.shex.sys.ValidationContext;

public class NodeConstraint
//extends ShapeExpression
implements Satisfies, ShexPrintable
{

    /*
    NodeConstraint  {
        id:shapeExprLabel?
        nodeKind:("iri" | "bnode" | "nonliteral" | "literal")?
        datatype:IRIREF?
        xsFacet*
        values:[valueSetValue+]?
    }
     */


    private List<NodeConstraintComponent> constraints = new ArrayList<>();

    public NodeConstraint(List<NodeConstraintComponent> constraints) {
        this.constraints = List.copyOf(constraints);
    }

    public List<NodeConstraintComponent> components() { return constraints; }

    static class NodeConstraintBuilder {
        NodeKindConstraint nodeKind;
        DatatypeConstraint datatype = null;
        List<NodeConstraint> facets = new ArrayList<>();
        ValueConstraint values;
    }


    @Override
    public boolean satisfies(ValidationContext vCxt, Node data) {
        for ( NodeConstraintComponent ncc : constraints ) {
            ReportItem item = ncc.nodeSatisfies(vCxt, data);
            if ( item != null ) {
                vCxt.reportEntry(item);
                return false;
            }
        }
        return true;
    }
}