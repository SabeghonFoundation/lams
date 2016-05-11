/*

   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */
package org.apache.batik.svggen;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Abstract class with common utility methods used by subclasses
 * for specific convertion operations. It holds a reference to a
 * domFactory Document, which many implementations use, and provides
 * a convenience method, to offers a convertion of double values
 * to String that remove the trailing '.' character on integral
 * values.
 *
 * @author <a href="mailto:vincent.hardy@eng.sun.com">Vincent Hardy</a>
 *
 */
public abstract class AbstractSVGFilterConverter
    implements SVGFilterConverter, ErrorConstants {
    /**
     * Used by converters to create Elements and other DOM objects
     */
    protected SVGGeneratorContext generatorContext;

    /**
     * Map of descriptions already processed by this converter. The
     * key type is left to the implementations
     */
    protected Map descMap = new HashMap();

    /**
     * Set of definitions to interpret the values of the attributes
     * generated by this converter since its creation
     */
    protected List defSet = new LinkedList();

    /**
     * @param generatorContext an be used by the SVGConverter extentions
     *        to create Elements and other types of DOM objects.
     */
    public AbstractSVGFilterConverter(SVGGeneratorContext generatorContext) {
        if (generatorContext == null)
            throw new SVGGraphics2DRuntimeException(ERR_CONTEXT_NULL);
        this.generatorContext = generatorContext;
    }

    /**
     * @return set of definitions referenced by the attribute
     *         values created by the implementation since its
     *         creation. The return value should never be null.
     *         If no definition is needed, an empty set should be
     *         returned.
     */
    public List getDefinitionSet(){
        return defSet;
    }

    /**
     * Utility method for subclasses.
     */
    public final String doubleString(double value) {
        return generatorContext.doubleString(value);
    }
}
