/**
 *   Copyright (c) Rich Hickey. All rights reserved.
 *   The use and distribution terms for this software are covered by the
 *   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 *   which can be found in the file epl-v10.html at the root of this distribution.
 *   By using this software in any fashion, you are agreeing to be bound by
 *       the terms of this license.
 *   You must not remove this notice, or any other, from this software.
 **/

/* rich Aug 21, 2007 */

package clojure.lang;

import java.lang.ref.Reference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.net.URLClassLoader;
import java.net.URL;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;

public class DynamicClassLoader extends URLClassLoader{

static final URL[] EMPTY_URLS = new URL[]{};

static {
    registerAsParallelCapable();
}


protected final LoaderContext context;

public DynamicClassLoader(ClassLoader parent){
    this(EMPTY_URLS,parent);
}

public DynamicClassLoader(URL[] urls, ClassLoader parent){
    this(urls, parent, parent instanceof DynamicClassLoader? ((DynamicClassLoader) parent).context : LoaderContext.ROOT);
}

public DynamicClassLoader(final URL[] urls, final ClassLoader parent, final LoaderContext loaderContext){
    super(urls, parent);
    this.context = loaderContext;
}

public void close() throws java.io.IOException{
    this.context.namespaces.clear();
    this.context.classCache.clear();
    super.close();
}

public Class defineClass(String name, byte[] bytes, Object srcForm){
    Util.clearCache(context.classCacheReferenceQueue, context.classCache);
    Class c = defineClass(name, bytes, 0, bytes.length);
    context.classCache.put(name, new SoftReference(c,context.classCacheReferenceQueue));
    return c;
}

static public Class<?> findInMemoryClass(LoaderContext context, String name) {
    Reference<Class> cr = context.classCache.get(name);
	if(cr != null)
		{
		Class c = cr.get();
        if(c != null)
            return c;
		else
	        context.classCache.remove(name, cr);
		}
	return null;
}

static public Class<?> findInMemoryClass(String name) {
    ClassLoader ccl = Thread.currentThread().getContextClassLoader();
    LoaderContext lc = null;
    do {
    	if (ccl instanceof DynamicClassLoader)
    		lc = ((DynamicClassLoader)ccl).context;
    	else {
    		ccl = ccl.getParent();
    		if (ccl == null)
    			lc = LoaderContext.ROOT;
    	}
    } while (lc == null);
    return findInMemoryClass(lc, name);
}

protected Class<?>findClass(String name) throws ClassNotFoundException {
	Class c = findInMemoryClass(context, name);
	if (c != null)
		return c;
	else
		return super.findClass(name);
}

protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
	Class c = findLoadedClass(name);
	if (c == null) {
		c = findInMemoryClass(context, name);
		if (c == null)
			c = super.loadClass(name, false);
    }
	if (resolve)
		resolveClass(c);
	return c;
}

public void registerConstants(int id, Object[] val){
	context.constantVals.put(id, val);
}

public Object[] getConstants(int id){
	return context.constantVals.get(id);
}

public void addURL(URL url){
        super.addURL(url);
}

}
