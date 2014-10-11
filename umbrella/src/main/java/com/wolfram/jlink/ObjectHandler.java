//////////////////////////////////////////////////////////////////////////////////////
//
//   J/Link source code (c) 1999-2000, Wolfram Research, Inc. All rights reserved.
//
//   Use is governed by the terms of the J/Link license agreement, which can be found at
//   www.wolfram.com/solutions/mathlink/jlink.
//
//   Author: Todd Gayley
//
//////////////////////////////////////////////////////////////////////////////////////

package com.wolfram.jlink;

import java.util.*;
import java.lang.reflect.*;


// ObjectHandler is the class used by KernelLinkImpl to manage all interactions with "byref" objects, including
// loading classes, putting object references, and invoking methods. Thus, ObjectHandler contains the list of loaded classes
// and the collection of loaded objects. The motivation for putting this state and behavior
// in a separate class is that it really belongs to a kernel, not to a link. Multiple links could share the same kernel,
// and they would need to share the same Java-side state w.r.t. loaded classes and objects. Otherwise two classes
// could get the same classIndex, and two objects could get the same object ID numbers. Another motivation is just
// organizational--KernelLinkImpl is big enough without all this code in it as well.
//
// Most calls in KernelLinkImpl that deal with objects or classes are just forwarded to this class for handling.
// ObjectHandler is not completely ignorant of KernelLink, although that would be nice. Some methods take a link as
// an argument, and write things onto it. Some very complex data has to be written for some of the behavior in this
// class (e.g., loadClass() and reflect()). It would be overy complicated to try to return that data to KernelLinkImpl
// and have it do the writing. No reading is done by ObjectHandler, though.
//
// This class is not a user-level class. Only programmers writing KernelLink implementations or related low-level code
// would ever need to be concerned with ObjectHandler.


public class ObjectHandler {

    protected JLinkClassLoader jlinkLoader = new JLinkClassLoader();

	protected InstanceCollection instanceCollection = new InstanceCollection();
	protected Map classCollection = Collections.synchronizedMap(new HashMap());
    // Sent back to M during putReference() and loadClass() to identify this runtime
    // (in case there are more than one running).
    protected String vmName;

	// Link from Java to FE for FE/Java kernel sharing (completes triangle).
	// This doesn't very logically belong to ObjectHandler, but it is a piece of
	// state that must be shared between the main javalink and the uilink, which makes
	// this a convenient class to put it in (and it is going away soon enough).
	protected MathLink feServerLink = null;

	MathLink getFEServerLink() {
		return feServerLink;
	}

	void setFEServerLink(MathLink feServerLink) {
		this.feServerLink = feServerLink;
	}


    public JLinkClassLoader getClassLoader() {
        return jlinkLoader;
    }

    public void setClassLoader(JLinkClassLoader jlinkLoader) {
        this.jlinkLoader = jlinkLoader;
    }


	public Class classFromID(int id) {

		Object obj = classCollection.get(new Integer(id));
		return obj != null ? ((ClassRecord) obj).getCls() : null;
	}


	public void putReference(MathLink ml, Object obj, Class upCastCls) throws MathLinkException {

		putRef(ml, obj, upCastCls, instanceCollection.keyOf(obj));
	}


	public Object getObject(String objSymbol) {

		Object result = null;

		if (!objSymbol.equals("Null")) {
			long key = keyFromMmaSymbol(objSymbol);
			result = instanceCollection.get(key);
			if (result == null)
			    // "Dummy" exception, caught by KernelLinkImpl.getObject() and turned into a MathLinkException.
			    throw new IllegalArgumentException("Symbol in getObject() does not reference a Java object");
		}
		return result;
	}


	public int loadClass(int classID, String className, Object objSupplyingClassLoader) throws ClassNotFoundException, SecurityException {

        ClassLoader cl = objSupplyingClassLoader == null ? jlinkLoader : objSupplyingClassLoader.getClass().getClassLoader();
		ClassRecord classRec = new ClassRecord(className, cl, vmName);
        classCollection.put(new Integer(classID), classRec);
        return classID;
	}


	public void putInfo(KernelLink ml, int classID, Object objSupplyingClassLoader) throws MathLinkException {

		ClassRecord clsRec = (ClassRecord) classCollection.get(new Integer(classID));
		// If objSupplyingClassLoader != null, we are probably currently in the process of loading the class for
		// this object. The only thing we send to M is its raw symbol name. I don't think it is possible for the key to be 0
		// if the object != null, but we'll handle the case anyway.
		long key = (objSupplyingClassLoader == null ? 0 : instanceCollection.keyOf(objSupplyingClassLoader));
		clsRec.putInfo(ml, key != 0 ? mmaSymbolFromKey(key) : "Null");
	}


	public Object callCtor(int classID, int[] indices, Object[] args)
			throws NoSuchMethodException, InvalidClassException, InstantiationException,
			        IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		ClassRecord clsRec = (ClassRecord) classCollection.get(new Integer(classID));
        if (clsRec == null)
            throw new InvalidClassException();
		return clsRec.callBestCtor(indices, args);
	}


	public Object callMethod(int classID, Object instance, int[] indices, Object[] args)
			throws IllegalAccessException, InvalidClassException, InvocationTargetException {

		ClassRecord clsRec = (ClassRecord) classCollection.get(new Integer(classID));
        if (clsRec == null)
            throw new InvalidClassException();
		return clsRec.callBestMethod(indices, instance, args);
	}


	public Object getField(int classID, Object instance, int index)
			throws IllegalAccessException, InvalidClassException, IllegalArgumentException, NoSuchMethodException {

		ClassRecord clsRec = (ClassRecord) classCollection.get(new Integer(classID));
        if (clsRec == null)
            throw new InvalidClassException();
		return clsRec.callField(false, instance, index, null);
	}


	public void setField(int classID, Object instance, int index, Object val)
			throws IllegalAccessException, InvalidClassException, IllegalArgumentException, NoSuchMethodException {

		ClassRecord clsRec = (ClassRecord) classCollection.get(new Integer(classID));
        if (clsRec == null)
            throw new InvalidClassException();
		clsRec.callField(true, instance, index, val);
	}


	public void releaseInstance(String[] objectSyms) {

		// None of objectSyms can be "Null".
		for (int i = 0; i < objectSyms.length; i++) {
			long key = keyFromMmaSymbol(objectSyms[i]);
			instanceCollection.remove(key);
		}
	}


	// Release all instances of a given class. Currently unused.
	public void releaseAllInstances(int classID) {

		String clsName = ((ClassRecord) classCollection.get(new Integer(classID))).getCls().getName();
        synchronized (instanceCollection) {
    		// Store keys in array first to avoid changing instanceCollection while an enumerator
    		// for it is active.
    		Long[] keyArray = new Long[instanceCollection.size()];
    		int i = 0;
    		for (Enumeration keys = instanceCollection.keys(); keys.hasMoreElements(); ) {
    			keyArray[i++] = (Long) keys.nextElement();
    		}
    		while (--i >= 0) {
    			Object val = instanceCollection.get(keyArray[i].longValue());
    			if (val.getClass().getName().equals(clsName))
    				instanceCollection.remove(keyArray[i].longValue());
    		}
        }
	}


	public void unloadClass(int classID) {

		classCollection.remove(new Integer(classID));
	}


	public void callOnLoadClass(KernelLink ml, int classID)
			throws IllegalArgumentException, IllegalAccessException, InvalidClassException, InvocationTargetException {

		ClassRecord clsRec = (ClassRecord) classCollection.get(new Integer(classID));
        if (clsRec == null)
            throw new InvalidClassException();
		clsRec.callOnLoadClass(ml);
	}


	public void callOnUnloadClass(KernelLink ml, int classID)
			throws IllegalArgumentException, IllegalAccessException, InvalidClassException, InvocationTargetException {

		ClassRecord clsRec = (ClassRecord) classCollection.get(new Integer(classID));
        if (clsRec == null)
            throw new InvalidClassException();
		clsRec.callOnUnloadClass(ml);
	}


	public int reflect(MathLink ml, int classID, int type, boolean includeInherited, boolean sendData)
	        throws InvalidClassException, MathLinkException {

		ClassRecord clsRec = (ClassRecord) classCollection.get(new Integer(classID));
        if (clsRec == null)
            throw new InvalidClassException();
		return clsRec.reflect(ml, type, includeInherited, sendData);
	}


	public void peekObjects(MathLink ml) throws MathLinkException {

        synchronized (instanceCollection) {
            ml.putFunction("List", instanceCollection.size());
            for (Enumeration keys = instanceCollection.keys(); keys.hasMoreElements(); ) {
                Long key = (Long) keys.nextElement();
                Object inst = instanceCollection.get(key.longValue());
                putRef(ml, inst, null, key.longValue());
            }
        }
		ml.endPacket();
	}


	public void peekClasses(MathLink ml) throws MathLinkException {

        synchronized (classCollection) {
            Set values = classCollection.keySet();
    		ml.putFunction("List", values.size());
    		Iterator iter = values.iterator();
    		while (iter.hasNext()) {
                // Sending back integers here.
    			ml.put(iter.next());
    		}
        }
		ml.endPacket();
	}


    public void setVMName(String name) {
        this.vmName = name;
    }

    public String getVMName() {
        return vmName;
    }


	public String getComponentTypeName(int classID) {

		ClassRecord clsRec = (ClassRecord) classCollection.get(new Integer(classID));
		return Utils.getArrayComponentType(clsRec.getCls()).getName();
	}


	////////////////////////////////////  Private  //////////////////////////////////////

    private static final boolean RAW_JAVA_OBJECTS = false;

	public void putRef(MathLink ml, Object obj, Class upCastCls, long key) throws MathLinkException {

		if (key != 0) {
			// Object is already in instanceCollection; no need to call Mma function createInstanceDefs.
			if (RAW_JAVA_OBJECTS)
                ml.put(key);
            else
                ml.putSymbol(mmaSymbolFromKey(key));
			return;
		}
		// Object wasn't there; put it in, and set key to be the actual key, returned by put.
		key = instanceCollection.put(obj);
		// Now we need to send back command to set up rules for the instance.
		Class cls = upCastCls == null ? obj.getClass() : upCastCls;
		int classID = -1;
        synchronized (classCollection) {
    		// It might seem inefficient to do a linear search through a HashMap of ClassRecords, but this is
    		// negligible compared to the MathLink transfer time and the time to call the Mathematica function
    		// createInstanceDefs.
            Set entries = classCollection.entrySet();
            Iterator iter = entries.iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                if (cls.equals(((ClassRecord) entry.getValue()).getCls())) {
                    classID = ((Integer) entry.getKey()).intValue();
                    break;
                }
            }
        }
		if (classID == -1) {
			String name = cls.getName();
			ml.putFunction(KernelLinkImpl.MMA_LOADCLASSANDCREATEINSTANCEDEFS, 3);
            ml.put(vmName);
            ml.put(name);
            if (RAW_JAVA_OBJECTS)
                ml.put(key);
            else
                ml.putSymbol(mmaSymbolFromKey(key));
		} else {
			ml.putFunction(KernelLinkImpl.MMA_CREATEINSTANCEDEFS, 3);
            ml.put(vmName);
			ml.put(classID);
            if (RAW_JAVA_OBJECTS)
                ml.put(key);
            else
                ml.putSymbol(mmaSymbolFromKey(key));
		}
        return;
	}


/////////////////////////  JavaObject to key translation  //////////////////////////

	// These are the functions that translate back and forth from the mma representation of a Java object
	// to the key used for lookup on the Java side--in effect, the Java-side representation. This is the
	// only place that cares about the details of this conversion, although the mma code does depend on the
	// representation being a symbol.
	// Currently, the mma symbol is of the form JLink`Objects`vmname`JavaObjectXXXXXX where XXXXXX is a
	// positive integer that gives the key used for looking up objects in the InstanceCollection.

	private static long keyFromMmaSymbol(String sym) {

		// Note that we are not prepared to handle "Null" here. Since null is never stored in
		// instanceCollection, amd there is no legitimate key for it, Null handling must be done
		// by the caller.
		// 't' because we are stripping off JLink`Objects`vmname`JavaObject.
		String keyString = sym.substring(sym.lastIndexOf('t') + 1);
		try {
			// Will always be > 0 (all instance index values sent to Mma are > 0).
			return Long.parseLong(keyString, 10);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private String mmaSymbolFromKey(long key) {
		return KernelLink.PACKAGE_CONTEXT + "Objects`" + vmName + "`JavaObject" + Long.toString(key);
	}

}   //// End of ObjectHandler


/*******************************************  ClassRecord  *************************************************/

class ClassRecord {

    private String vmName;
	private String name;
	private Class cls;
	private Constructor[] ctors;
	private Method[] methods;
	private Field[] fields;
	private Class componentType;  // != null if class is an array class. Stores the type at the leaves of the array.
	private int depth;   // only valid for arrays


	ClassRecord(String name, ClassLoader loader, String vmName) throws ClassNotFoundException, SecurityException {

        if (loader == null)
            loader = JLinkClassLoader.getInstance();
        // This used to be loader.loadClass(name), but that broke for classes like "[I" in Java 6.
        // Sun says "that wasn't how loadClass() was supposed to work; use Class.forName() instead."
        // in http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6387908. Not really sure whether
        // 2nd arg should be true or false. False seems closer to the original implementation.
		cls = Class.forName(name, false, loader);

        this.vmName = vmName;
		this.name = cls.getName();

		// We don't need to do anything differently for an interface vs. a class.

		ctors = cls.getConstructors(); // Publics only
		methods = cls.getMethods(); // Publics only
		fields = cls.getFields(); // Publics only

		// Methods that return objects can return ones of classes that are not public. This requires some special attention.
		// Below, getFoo() returns an object of class FooSubclass that is in the same package as obj's class but not public.
		// The f object is typed as Foo, and all Foo methods can be invoked on it, but its runtime type is FooSubclass
		//    Foo f = obj.createFoo();
		// J/Link has no notion of declared type, only runtime type, so if you try to call methods from Mathematica on f,
		// you get an IllegalAccessException because f's class (FooSubclass) is not public.
		// An example of this pattern is the Communications API (javax.comm package). Factory methods create instances of classes
		// that implement interfaces, but these classes are not themselves public. As long as you only refer to these objects
		// via their interface types, there is no problem. But of course J/Link sees them only as their true types.
		// This is a thorny problem for all advanced uses of the reflection API, so Sun provided a special means to deal with
		// it in JDK1.2: the java.lang.reflect.AccessibleObject class (this class is motivated by a number of reflection
		// issues, not just this subtle one). Here we force the public methods and fields (not sure if this is an issue for
		// fields) of such non-public classes to be accessible. Note that we are not giving Mathematica access to non-public
		// members (the methods and fields arrays contain only publics); we are just allowing access to these methods via
		// an object reference whose type is a non-public class (yes, non-public classes can have public members).
		// For Java 1.x, users are out of luck; some calls that work fine from Java code (because of compile-time typing)
		// will generate an IllegalAccessException when called from Mathematica.
		int m = cls.getModifiers();
		if (!Modifier.isPublic(m)) {
			try {
				AccessibleObject.setAccessible(methods, true);
				AccessibleObject.setAccessible(fields, true);
			} catch (SecurityException e) {
				// Using AccessibleObject is governed by the security manager. These calls could fail.
				// Not much to do if that happens except issue a warning.
				System.err.println("Warning: the non-public class " + name + " was loaded by J/Link, and the attempt " +
											"to make its public methods accessible from Mathematica failed due to a Java " +
											"security restriction. Objects of this class may generate an IllegalAccessException  " +
											"when they are used from Mathematica. See the documentation for the class " +
											"java.lang.reflect.ReflectPermission.");
			}
		}

		componentType = Utils.getArrayComponentType(cls);
		if (componentType != null) {
			// Is an array class.
			if (MathLinkImpl.DEBUGLEVEL > 0) if (name.charAt(0) != '[') System.err.println("Array class whose name didn't start with [");
			depth = 0;
			while (name.charAt(depth) == '[')
				depth++;
		}
	}

	Class getCls() {
		return cls;
	}

	// This is the method that returns the data structure describing a class and its methods, fields, ctors. It is called from
	// Mathematica inside LoadClass[].
	void putInfo(KernelLink kl, String symbolNameOfObjSupplyingClassLoader) throws MathLinkException {

		// We use an Expr to build the result until we are finished instead of sending directly onto the link,
	    // since it is possible for a NoClassDefFoundError, or something like this, to be thrown by these machinations.
	    // This would be easier with a loopback link, but we want J/Link to function without the native library
	    // loaded.
	    // Nothing that happens with this on-demand class loading is fatal, so we don't want any Errors to be thrown.

		// Create an Expr with this form:
		//   {"className", JLink`Private`loadClassFromJava["vmname", "superclass", objSupplyingClsLoader], isInterface, ctors, methods, fields}
		//
	    Expr classNameArg = null;
	    Expr loadClassArg = null;
	    Expr isInterfaceArg = null;
	    Expr ctorsArg = null;
	    Expr methodsArg = null;
	    Expr fieldsArg = null;

		Error err = null;
		Class complexClass = kl.getComplexClass();

		try {
			classNameArg = new Expr(name);

			loadClassArg = new Expr(new Expr(Expr.SYMBOL, KernelLinkImpl.MMA_LOADCLASS), new Expr[]{});
			loadClassArg = loadClassArg.insert(new Expr(vmName), 1);
            Class sup = cls.getSuperclass();
            loadClassArg = loadClassArg.insert(sup == null ? Expr.SYM_NULL : new Expr(sup.getName()), 2);
            // We only put the symbol name, not put(Object). If the name != "Null", then we are probably already in the
            // middle of putReference()/LoadClass[] on this object anyway.
            loadClassArg = loadClassArg.insert(new Expr(Expr.SYMBOL, symbolNameOfObjSupplyingClassLoader), 3);

            isInterfaceArg = cls.isInterface() ? Expr.SYM_TRUE : Expr.SYM_FALSE;

			// For each ctor, send: {index, declaration, paramType1, paramType2, ...}.
			if (componentType != null) {
                // Array ctors are fakes. index 0 --> int list argument specifying lengths in each dim.
                // index 1 --> int argument specifying length (only exists for 1-deep arrays).
			    ctorsArg = new Expr(Expr.SYM_LIST, new Expr[]{
			                    new Expr(Expr.SYM_LIST, new Expr[]{
			                            new Expr(0),
			                            new Expr(""),
			                            new Expr(classToMathLinkConstant(int[].class, complexClass, true))
			                    })
			                });
                if (depth == 1) {
                    ctorsArg = ctorsArg.insert(new Expr(Expr.SYM_LIST, new Expr[]{
                                        new Expr(1),
                                        new Expr(""),
                                        new Expr(classToMathLinkConstant(int.class, complexClass, true))
                                }), 2);
                }
			} else {
                Expr[] ctorExprs = new Expr[ctors.length];
				for (int i = 0; i < ctors.length; i++) {
					Class[] params = ctors[i].getParameterTypes();
					boolean hasAmbiguity = hasRealIntAmbiguity(i, params, ctors);
                    Expr[] thisCtorArgs = new Expr[params.length + 2];
                    thisCtorArgs[0] = new Expr(i);
                    thisCtorArgs[1] = new Expr(ctors[i].toString());
					// Put the types
					for (int j = 0; j < params.length; j++)
					    thisCtorArgs[j+2] = new Expr(classToMathLinkConstant(params[j], complexClass, hasAmbiguity));
					ctorExprs[i] = new Expr(Expr.SYM_LIST, thisCtorArgs);
				}
				ctorsArg = new Expr(Expr.SYM_LIST, ctorExprs);
			}

	 		// For methods, send: {index, declaration, isStatic, name, params...}
	 		// With hierarchical method of loading classes, we only need to send method info for
	 		// methods declared in this class, statics that aren't duplicates of an inherited method,
	 		// and methods with names that match inherited ones (i.e., overloaded [not _overridden_] ones).
 			Method[] declMethods = cls.getDeclaredMethods();
			String[] names = new String[declMethods.length];
			for (int i = 0; i < declMethods.length; i++)
			 	names[i] = declMethods[i].getName();
			int numMethods = methods.length;
			int sentMethodCount = 0;

			List methodExprs = new ArrayList(numMethods);
            for (int i = 0; i < numMethods; i++) {
                Method meth = methods[i];
                boolean isStatic = Modifier.isStatic(meth.getModifiers());
                boolean sendThisMethod = false;
                if (meth.getDeclaringClass() == cls) {
                    sendThisMethod = true;
                } else if (isStatic) {
                    // For statics, we want to send them unless they are an exact override of an inherited
                    // static method. Such methods show up twice in meths, and the inherited version would
                    // overwrite the subclass version in Mathematica.
                    sendThisMethod = true;
                    String name = meth.getName();
                    Class[] params = meth.getParameterTypes();
                    staticFilter:
                    for (int j = 0; j < declMethods.length; j++) {
                        if (name.equals(names[j])) {
                            Class[] otherParams = declMethods[j].getParameterTypes();
                            if (otherParams.length == params.length) {
                                sendThisMethod = false;
                                for (int k = 0; k < params.length; k++) {
                                    if (params[k] != otherParams[k]) {
                                        sendThisMethod = true;
                                        break staticFilter;
                                    }
                                }
                            }
                        }
                    }
                // See http://stackoverflow.com/questions/1961350/problem-in-the-getdeclaredmethods-java
                // for where I got this isBridge() call from. This is a fix for bug 239267.
                } else if (!meth.isBridge()) {
                    String name = meth.getName();
                    for (int j = 0; j < declMethods.length; j++) {
                        if (name.equals(names[j])) {
                            sendThisMethod = true;
                            break;
                        }
                    }
                }
                if (sendThisMethod) {
                    sentMethodCount++;
                    Class[] params = meth.getParameterTypes();
                    boolean hasAmbiguity = hasRealIntAmbiguity(i, params, methods);
                    Expr[] thisMethodArgs = new Expr[params.length + 4];
                    thisMethodArgs[0] = new Expr(i);
                    thisMethodArgs[1] = new Expr(meth.toString());
                    thisMethodArgs[2] = isStatic ? Expr.SYM_TRUE : Expr.SYM_FALSE;
                    thisMethodArgs[3] = new Expr(meth.getName());
                    for (int j = 0; j < params.length; j++)
                        thisMethodArgs[j+4] = new Expr(classToMathLinkConstant(params[j], complexClass, hasAmbiguity));
                    methodExprs.add(new Expr(Expr.SYM_LIST, thisMethodArgs));
                }
            }
            methodsArg = new Expr(Expr.SYM_LIST, (Expr[]) methodExprs.toArray(new Expr[sentMethodCount]));

	 		// For fields, send: {index, isStatic, type as string, name, type as int}
 			Field[] declFields = cls.getDeclaredFields();
			names = new String[declFields.length];
			for (int j = 0; j < declFields.length; j++)
				names[j] = declFields[j].getName();
	 		int numFields = fields.length;
	 		int sentFieldCount = 0;
            List fieldExprs = new ArrayList(numFields);

	 		for (int i = 0; i < numFields; i++) {
	 			boolean sendThisField = true;
				Field fld = fields[i];
	 			boolean isStatic = Modifier.isStatic(fld.getModifiers());
				if (isStatic && fld.getDeclaringClass() != cls) {
		 			// For statics, we want to send them unless they are a duplicate of an inherited
		 			// static field (this actually does happen in some classes). Such fields show up twice
		 			// in 'fields', and would cause problems in Mathematica.
					String name = fld.getName();
					for (int j = 0; j < declFields.length; j++) {
						if (name.equals(names[j])) {
							sendThisField = false;
							break;
						}
					}
				}
				if (sendThisField) {
				    sentFieldCount++;
                    Expr[] thisFieldArgs = new Expr[5];
                    thisFieldArgs[0] = new Expr(i);
                    thisFieldArgs[1] = isStatic ? Expr.SYM_TRUE : Expr.SYM_FALSE;
                    thisFieldArgs[2] = new Expr(fld.getType().toString());
                    thisFieldArgs[3] = new Expr(fld.getName());
                    thisFieldArgs[4] = new Expr(classToMathLinkConstant(fld.getType(), complexClass, false));
                    fieldExprs.add(new Expr(Expr.SYM_LIST, thisFieldArgs));
				}
	 		}
            fieldsArg = new Expr(Expr.SYM_LIST, (Expr[]) fieldExprs.toArray(new Expr[sentFieldCount]));

		} catch (Error e) {
	 		err = e;
	 	} finally {
	 		if (err == null) {
                Expr classInfo = new Expr(Expr.SYM_LIST, new Expr[]{classNameArg, loadClassArg, isInterfaceArg, ctorsArg, methodsArg, fieldsArg});
                kl.put(classInfo);
	 		} else {
                kl.message("Java::excptn", err.toString());
                kl.putSymbol("$Failed");
	 		}
	 	}
	}


	private static int classToMathLinkConstant(Class cls, Class complexClass, boolean hasRealIntAmbiguity) {

		int res = 0;
		if (cls.isPrimitive()) {
			if (cls == int.class) {
				res = MathLink.TYPE_INT;
			} else if (cls == double.class) {
				res = hasRealIntAmbiguity ? MathLink.TYPE_DOUBLE : KernelLinkImpl.TYPE_DOUBLEORINT;
			} else if (cls == boolean.class) {
				res = MathLink.TYPE_BOOLEAN;
			} else if (cls == byte.class) {
				res = MathLink.TYPE_BYTE;
			} else if (cls == char.class) {
				res = MathLink.TYPE_CHAR;
			} else if (cls == short.class) {
				res = MathLink.TYPE_SHORT;
			} else if (cls == long.class) {
				res = MathLink.TYPE_LONG;
			} else if (cls == float.class) {
				res = hasRealIntAmbiguity ? MathLink.TYPE_FLOAT : KernelLinkImpl.TYPE_FLOATORINT;
			}
		} else if (cls == String.class) {
			res = MathLink.TYPE_STRING;
		} else if (cls == complexClass) {
			res = MathLink.TYPE_COMPLEX;
		} else if (cls.isArray()) {
			res = KernelLinkImpl.TYPE_ARRAY1 + classToMathLinkConstant(cls.getComponentType(), complexClass, hasRealIntAmbiguity);
		} else if (cls == java.math.BigInteger.class) {
			res = KernelLinkImpl.TYPE_BIGINTEGER;
		} else if (cls == java.math.BigDecimal.class) {
			res = KernelLinkImpl.TYPE_BIGDECIMAL;
		} else if (cls == Expr.class) {
			res = KernelLinkImpl.TYPE_EXPR;
		} else {
			res = KernelLinkImpl.TYPE_OBJECT;
		}
		return res;
	}


	private static boolean hasRealIntAmbiguity(int thisIndex, Class[] params, Member[] members) {

		String thisName = members[thisIndex].getName();
		boolean hasRealParam = false;
		for (int j = 0; j < params.length; j++) {
			Class c = params[j];
			if (c.isArray())
				c = Utils.getArrayComponentType(c);
			if (c == float.class || c == double.class) {
				hasRealParam = true;
				break;
			}
		}
		boolean isMethod = members.length > 0 && members[0] instanceof Method;
		if (hasRealParam) {
			// This could be made more efficient than O(# meths * # meths that take a real arg), but most classes don't have
			// many methods, or at least not many methods that take a float or double param.
			for (int j = 0; j < members.length; j++) {
				Member otherMember = members[j];
				if (j != thisIndex && otherMember.getName().equals(thisName)) {
					Class[] otherParams = isMethod ? ((Method) otherMember).getParameterTypes() : ((Constructor) otherMember).getParameterTypes();
					if (otherParams.length == params.length) {
						for (int k = 0; k < params.length; k++) {
							Class p = params[k];
							Class op = otherParams[k];
							if (p.isArray() && op.isArray()) {
								p = Utils.getArrayComponentType(p);
								op = Utils.getArrayComponentType(op);
							}
							if ((p == float.class || p == double.class) &&
									(op == int.class || op == long.class || op == short.class || op == char.class || op == byte.class)) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}


	synchronized Object callField(boolean isSet, Object instance, int fieldIndex, Object val)
			throws IllegalAccessException, NoSuchMethodException {

		// Instance will be null for statics.
		Field f = fields[fieldIndex];
		if (isSet) {
			f.set(instance, val);
			return null; // Will be ignored.
		} else {
			// Get
			return f.get(instance);
		}
	}


	private static final int NOT			= 0;
	private static final int ASSIGNABLE	= 1;
	private static final int EXACTLY		= 2;


	Object callBestCtor(int[] indices, Object[] args)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		Object res = null;

		if (componentType != null) {
			// Is an array class
			int[] dims;
			if (indices[0] == 0) {
				// 0th "ctor" is the one with {lengths} arg.
				dims = (int[]) args[0];
			} else {
				// 1th "ctor" is the one with length arg.
				dims = new int[1];
				dims[0] = ((Integer) args[0]).intValue();
			}
			res = Array.newInstance(componentType, dims);
		} else {
			// Not an array class
			// bestMember may modify args array in place.
			Constructor ctor = (Constructor) bestMember(indices, args, false);
			res = ctor.newInstance(args);
		}
		return res;
	}


	Object callBestMethod(int[] indices, Object instance, Object[] args)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		// bestMember may modify args array in place.
		Method meth = (Method) bestMember(indices, args, true);
		return meth.invoke(instance, args);
	}


	private Object bestMember(int[] indices, Object[] args, boolean isMethod) {

		// Note that "object" in this method refers to arrays as well as "real" objects.

		int len = indices.length;

		if (len == 1)
			return isMethod ? (Object) methods[indices[0]] : (Object) ctors[indices[0]];

		Class[] argClasses = new Class[args.length];
		for (int i = 0; i < args.length; i++)
			argClasses[i] = args[i].getClass();

		boolean[] objsAssignable = new boolean[len];
		boolean[] primitivesMatch = new boolean[len];
		boolean atLeastOneMethodMatchesObjects = false;

		for (int i = 0; i < len; i++) {
			Class[] paramClasses = isMethod ? methods[indices[i]].getParameterTypes() : ctors[indices[i]].getParameterTypes();
			int objsMatch = objectClassesMatch(argClasses, paramClasses);
			boolean primsMatch = primitiveClassesMatch(argClasses, paramClasses);
			if (objsMatch == EXACTLY && primsMatch) {
				// Leave all this mess behind as soon as we find an exact match.
				return isMethod ? (Object) methods[indices[i]] : (Object) ctors[indices[i]];
			}
			if (objsMatch == EXACTLY || objsMatch == ASSIGNABLE) {
				objsAssignable[i] = true;
				atLeastOneMethodMatchesObjects = true;
			}
			if (primsMatch)
				primitivesMatch[i] = true;
		}

		// Getting here means that no method/ctor had an _exact_ match for objects and primitives.

		// If there were none whose objects matched in a callable way, bail.
		if (!atLeastOneMethodMatchesObjects) {
			// Call a method just to get the correct exception delivered to Mathematica.
			return isMethod ? (Object) methods[indices[0]] : (Object) ctors[indices[0]];
		}

		// If there were ctors/methods whose objects matched in the "isAssignableFrom" sense, and whose primitives matched
		// exactly, then call the first one.
		for (int i = 0; i < len; i++)
			if (objsAssignable[i] && primitivesMatch[i])
				return isMethod ? (Object) methods[indices[i]] : (Object) ctors[indices[i]];

		// Getting here means that there were ctors whose objects matched in the "isAssignableFrom" sense, but
		// whose primitives need to be "massaged". This can result from several scenarios, for example:
		//      Class has ctor sigs  (int, Object1)  and  (byte, Object2)   where Object1 and Object2 are not related.
		// If the ctor is called from M with Object2, the args will be read as (INT, OBJECT). The int arg will be too
		// wide for the Object2 form. It is presumably rare to get this far.
		Object[] newArgs = new Object[args.length];
		for (int i = 0; i < len; i++) {
			// This test against objsAssignable isn't part of the if() loop test above because of an incredible bug
			// in Java 1.4.0 on Windows (at least).
			if (!objsAssignable[i])
				continue;
			// It's safe it re-use newArgs array on each pass.
			System.arraycopy(args, 0, newArgs, 0, args.length);
			Class[] paramClasses = isMethod ? methods[indices[i]].getParameterTypes() : ctors[indices[i]].getParameterTypes();
			if (massagePrimitives(argClasses, paramClasses, newArgs)) {
				// Overwrite the passed-in args array with the new massaged args.
				System.arraycopy(newArgs, 0, args, 0, args.length);
				return isMethod ? (Object) methods[indices[i]] : (Object) ctors[indices[i]];
			}
		}

		// Fall-through to here means that there was no appropriate ctor for the args.
		// This should only happen on a user error (passing bad args), not a failing of J/Link to disambiguate among
		// potential ctors. Pick an arbitrary ctor from indices[] just to get the proper exception thrown when it is called.
		if (MathLinkImpl.DEBUGLEVEL > 0) System.err.println("Bad fall-through in bestMember");
		return isMethod ? (Object) methods[indices[0]] : (Object) ctors[indices[0]];
	}


	// Objects and arrays
	private static int objectClassesMatch(Class[] argClasses, Class[] paramClasses) {

		int len = argClasses.length;  // paramClasses is the same length
		int match = EXACTLY;
		for (int i = 0; i < len; i++) {
			Class argCls = argClasses[i];
			Class paramCls = paramClasses[i];
			if (!paramCls.isPrimitive()) {
				// Note that == is an OK test for Class objects; they are singletons.
				if (paramCls == argCls) {
					// Do nothing
				} else if (paramCls.isAssignableFrom(argCls)) {
					match = ASSIGNABLE;
				} else {
					return NOT;
				}
			}
		}
		return match;
	}

	private static boolean primitiveClassesMatch(Class[] argClasses, Class[] paramClasses) {

		int len = argClasses.length;  // paramClasses is the same length
		for (int i = 0; i < len; i++) {
			boolean argsMatch = true;
			Class argCls = argClasses[i];
			Class paramCls = paramClasses[i];
			if (paramCls.isPrimitive()) {
				// Tests are done in an order that I hope reflects their commonness of usage.
				if (paramCls == int.class) {
					argsMatch = argCls == Integer.class;
				} else if (paramCls == double.class) {
					argsMatch = argCls == Double.class;
				} else if (paramCls == boolean.class) {
					argsMatch = argCls == Boolean.class;
				} else if (paramCls == byte.class) {
					argsMatch = argCls == Byte.class;
				} else if (paramCls == long.class) {
					argsMatch = argCls == Long.class;
				} else if (paramCls == float.class) {
					argsMatch = argCls == Float.class;
				} else if (paramCls == short.class) {
					argsMatch = argCls == Short.class;
				} else if (paramCls == char.class) {
					argsMatch = argCls == Character.class;
				}
			}
			if (!argsMatch)
				return false;
		}
		return true;
	}

	// Checks whether primitives can be narrowed without losing information, and does the narrowing, stuffing
	// the new args into newArgs. Args that need no modification can be ignored, as newArgs is already filled
	// with the original args.
	private static boolean massagePrimitives(Class[] argClasses, Class[] paramClasses, Object[] newArgs) {

		for (int i = 0; i < newArgs.length; i++) {
			Class paramCls = paramClasses[i];
			Class argCls = argClasses[i];
			if (paramCls.isPrimitive()) {
				// There is no need to check if incoming arg is narrower than param. It is ensured by M code that
				// the arg is the widest among all signatures at that position. Therefore we also do not need to check
				// params that are long or double, since they are the largest in their categories.
				if (paramCls == int.class) {
					if (argCls == Long.class) {
						long val = ((Number) newArgs[i]).longValue();
						if (val <= Integer.MAX_VALUE && val >= Integer.MIN_VALUE)
							newArgs[i] = new Integer((int) val);
						else
							return false;
					}
				} else if (paramCls == short.class) {
					if (argCls == Long.class || argCls == Integer.class) {
						long val = ((Number) newArgs[i]).longValue();
						if (val <= Short.MAX_VALUE && val >= Short.MIN_VALUE)
							newArgs[i] = new Short((short) val);
						else
							return false;
					}
				} else if (paramCls == char.class) {
					if (argCls == Long.class || argCls == Integer.class || argCls == Short.class) {
						long val = ((Number) newArgs[i]).longValue();
						if (val <= Character.MAX_VALUE && val >= Character.MIN_VALUE)
							newArgs[i] = new Character((char) val);
						else
							return false;
					}
				} else if (paramCls == byte.class) {
					if (argCls == Long.class || argCls == Integer.class || argCls == Short.class || argCls == Character.class) {
						long val = argCls == Character.class ? ((Character) newArgs[i]).charValue() : ((Number) newArgs[i]).longValue();
						if (val <= Byte.MAX_VALUE && val >= Byte.MIN_VALUE)
							newArgs[i] = new Byte((byte) val);
						else
							return false;
					}
				} else if (paramCls == float.class) {
					if (argCls == Double.class) {
						double d = Math.abs(((Double) newArgs[i]).doubleValue());
						if (d <= Float.MAX_VALUE && d >= Float.MIN_VALUE)
							newArgs[i] = new Float((float) d);
						else
							return false;
					}
				}
			}
		}
		return true;
	}


	// If class implements a function onLoadClass(KernelLink), call it. Make no attempt to handle exceptions;
	// this is done at higher level.
	void callOnLoadClass(KernelLink ml)
			throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {

		for (int i = 0; i < methods.length; i++) {
			String name = methods[i].getName();
			if (name.equals("onLoadClass")) {
				Class[] params = methods[i].getParameterTypes();
				if(params.length == 1 && params[0].isInstance(ml)) {
					Object[] args = {ml};
					methods[i].invoke(null, args);
				}
			}
		}
	}


	private void fillObjectArrayFromCtor(Object array, Constructor compCtor, int[] dims, int depth)
			throws InvocationTargetException, IllegalAccessException, InstantiationException {

		// depth is an index into dims--it counts from 0 upward as we descend into the depths of the array.
		int i;
		if (depth == dims.length - 1)
			// We've reached the last dimension.
			for (i = 0; i < dims[depth]; i++)
				Array.set(array, i, compCtor.newInstance(null));
		else
			for (i = 0; i < dims[depth]; i++)
				fillObjectArrayFromCtor(((Object[]) array)[i], compCtor, dims, depth + 1);
	}


	// If class implements a function onUnloadClass(MathLink), call it. Make no attempt to handle exceptions;
	// this is done at higher level.
	void callOnUnloadClass(KernelLink ml)
			throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {

		for (int i = 0; i < methods.length; i++) {
			String name = methods[i].getName();
			if (name.equals("onUnloadClass")) {
				Class[] params = methods[i].getParameterTypes();
				if(params.length == 1 && params[0].isInstance(ml)) {
					Object[] args = {ml};
					methods[i].invoke(null, args);
				}
			}
		}
	}


	int reflect(MathLink ml, int type, boolean includeInherited, boolean sendData) throws MathLinkException {

		int num = 0;
		// type from same set as callJava uses: 1 == ctor, 2 == method, 3 == field.
		switch (type) {
			case 1:
				num = ctors.length;
				if (sendData) {
					for (int i = 0; i < num; i++) {
		 				ml.put(ctors[i].toString());
					}
				}
				break;
			case 2:
				for (int i = 0; i < methods.length; i++) {
					if (includeInherited || methods[i].getDeclaringClass() == cls) {
						num++;
						if (sendData)
							ml.put(methods[i].toString());
					}
				}
				break;
			case 3:
				for (int i = 0; i < fields.length; i++) {
					if (includeInherited || fields[i].getDeclaringClass() == cls) {
						num++;
						if (sendData) {
							boolean isStatic = Modifier.isStatic(fields[i].getModifiers());
							boolean isFinal = Modifier.isFinal(fields[i].getModifiers());
							String typeStr = fields[i].getType().toString();
							if (typeStr.startsWith("class "))
								typeStr = typeStr.substring(6);
							ml.put((isStatic ? "static " : "") + (isFinal ? "final " : "") + typeStr + " " + fields[i].getName());
						}
					}
				}
				break;
		}
		return num;
	}

}


/////////////////////////////////  InstanceCollection  /////////////////////////////////////////

// This is the class that holds the set of objects that are referenced in Mathematica. When objects are
// sent to Mathematica by reference, they get put in here; when ReleaseObject is called in Mathematica,
// they get removed from here.

class InstanceCollection {

	private Hashtable table;

	InstanceCollection() {
		table = new Hashtable(541);  // Increase the default size. 541 == 100th prime number.
	}

	// Return the long code that is object's key.
	// Returns 0 for "not there" and non-zero to give the key. Needless to say, we must
	// never let a key be 0 (this is handled elsewhere).
	synchronized long keyOf(Object obj) {

		int hash = getHashCode(obj);
		// This is the one int value that remains negative after abs(), so we must change it if it crops up.
		if (hash == Integer.MIN_VALUE)
			hash++;
		int posHash = Math.abs(hash);
		Bucket b = (Bucket) table.get(new Integer(posHash));
		if (b == null)
			return 0;
		else {
			Integer bucketKey = b.keyOf(obj);
			return bucketKey == null ? 0 : (((long) posHash) << 24) | bucketKey.intValue();
		}
	}

	synchronized Object get(long key) {

		if (key == 0)
			return null;
		int keyInt = (int) (key >> 24); // The object's hashcode (made positive)
		Bucket b = (Bucket) table.get(new Integer(keyInt));
		Integer bucketKey = new Integer((int) (key & 0x00FFFFFF)); // The within-bucket key
		return b.get(bucketKey);
	}

	// Returns the key. Only call this if you know object is not already in there. Otherwise you'll
	// end up with the same object stored more than once. This is not a serious problem, however.
	synchronized long put(Object obj) {

		int hash = getHashCode(obj);
		// This is the one int value that remains negative after abs(), so we must change it if it crops up.
		if (hash == Integer.MIN_VALUE)
			hash++;
		int posHash = Math.abs(hash);
		Integer keyInt = new Integer(posHash);
		Bucket b = (Bucket) table.get(keyInt);
		if (b == null) {
			// No one has yet used this hashcode.
			b = new Bucket();
			table.put(keyInt, b);
		} else {
			// Uncommon but OK; two stored objects had same hashcode.
			if (MathLinkImpl.DEBUGLEVEL > 0) System.err.println("Duplicate hashcodes of stored objects.");
		}
		// bucketKey can never be 0, so no overall key can be 0.
		Integer bucketKey = (Integer) b.put(obj);
		return (((long) posHash) << 24) | bucketKey.intValue();
	}

	synchronized void remove(long key) {

		// Safe to call (no exception thrown) if key is not in hashtable.
		Integer outerKey = new Integer((int) (key >> 24)); // The object's hashcode (made positive)
		Bucket b = (Bucket) table.get(outerKey);
		if (b != null) {
			if (b.size() == 1) {
				// Remove the whole bucket.
				table.remove(outerKey);
			} else {
				// Just remove the obj from its bucket.
				Integer bucketKey = new Integer((int) (key & 0x00FFFFFF)); // The within-bucket key
				b.remove(bucketKey);
			}
		}
	}

	synchronized int size() {
		Enumeration iter = keys();
		int count = 0;
		while (iter.hasMoreElements()) {
			count++;
			iter.nextElement();
		}
		return count;
	}

	synchronized Enumeration keys() {
		return new InstanceCollectionKeyEnumerator(table);
	}

	// Enumerates the _keys_ (the overall, Long, keys).
	class InstanceCollectionKeyEnumerator implements Enumeration {

		Hashtable table;
		Enumeration hashKeys;
		Integer currHashKey;
		Enumeration keysInCurrentBucket;

		InstanceCollectionKeyEnumerator(Hashtable collectionTable) {
			table = collectionTable;
			hashKeys = collectionTable.keys();
			currHashKey = null;
			keysInCurrentBucket = null;
		}

		public boolean hasMoreElements() {
			return (keysInCurrentBucket != null && keysInCurrentBucket.hasMoreElements())
					 || hashKeys.hasMoreElements();
		}

		// This returns the Long key, not the object itself.
		public Object nextElement() {
			if (keysInCurrentBucket == null || !keysInCurrentBucket.hasMoreElements()) {
				// Move to next bucket.
				if (!hashKeys.hasMoreElements())
					return null;
				currHashKey = (Integer) hashKeys.nextElement();
				keysInCurrentBucket = ((Bucket) table.get(currHashKey)).keys();
			}
			Integer withinBucketKey = (Integer) keysInCurrentBucket.nextElement();
			return new Long(((currHashKey.longValue()) << 24) | withinBucketKey.intValue());
		}
	}

	// Bit of a hack. We want to avoid calling the Expr class hashCode() method, as it can be expensive.
	// Instead we call a method for that class that amounts to super.hashCode(). This is perfectly legit--
	// we don't have any reason to prefer hashCode() over any other method that provides a reasonably
	// consistent int value from an object. In fact, for Expr, since hashCode() comes from the Expr's value,
	// it is possible to generate many Exprs that have the same hash, hurting the performance of keyOf()
	// if these objects are returned to Mathematica. This is another reason it is better to use
	// super.hashCode() for the Expr class.
	private static int getHashCode(Object obj) {
		return obj instanceof Expr ? ((Expr) obj).inheritedHashCode() : obj.hashCode();
	}

}


// Buckets are hashtables that use a key that is just an index count, ever growing. We have already used the
// object's hashCode() value as the key in the InstanceCollection hashtable, so there is no other way to get
// information out of the objects themselves to use as a key. The Integer within-bucket keys are stored as
// 24-bit numbers, so if a single bucket ever gets more than 2^24 elements, then repeat
// indices will occur and problems. Not a big concern, though, as 2^24 object references in Mathematica will
// consume about 50 Gb. Also, object lookup is slow (meaning the keyOf operation, which hunts based on the object
// itself, not its key), being a linear search, so if a bucket gets a very large number of elements things will
// be very slow for that reason as well. This type of search is used when objects are sent to M, not by method calls.
// Synchronization is not a concern because this class is always manipulated within a synchronized method
// of InstanceCollection.
//
class Bucket extends Hashtable {

	private int nextKey;
	private static final int largestKey = (1 << 24) - 1;  // Largest number that can be represented in 24 bits.

	Bucket() {
		// Collisions are unlikely, so use a small initial size.
		super(7, 1.0f);
		// Start at 1. This is just to ensure that no overall key can ever be 0, even if a hashcode is 0.
		nextKey = 1;
	}

	// Returns int code that is key within this bucket.
	Integer put(Object obj) {

		Integer withinBucketKey = new Integer(nextKey++);
		if (nextKey > largestKey)
			nextKey = 1;  // Start back at 1.
		put(withinBucketKey, new BucketRec(obj, withinBucketKey));
		return withinBucketKey;
	}

	public Object get(Object key) {
		BucketRec r = (BucketRec) super.get(key);
		return r != null ? r.obj : null;
	}

	// Returns Integer code that is key within this bucket.
	// Returns null for "not there". These numbers will not exceed the 24-bit
	// limit expected of them by the calling code. This method here is the bottleneck
	// in the case where buckets get a very large number of objects (say thousands).
	// Iterating over the Enumeration is expensive. Perhaps one of the Java 2 collection
	// classes will be faster. Investigate this when we drop support for Java 1.1.
	Integer keyOf(Object obj) {
		Enumeration elts = elements();
		while (elts.hasMoreElements()) {
			BucketRec d = (BucketRec) elts.nextElement();
			if (d.obj == obj)
				return d.key;
		}
		return null;
	}

	// This class is the element within the Bucket hashtable. We associate the object and its key so that
	// we can improve performance of reverse lookups (get key from object, done in keyOf()).
	class BucketRec {

		Object obj;
		Integer key;

		BucketRec(Object obj, Integer key) {
			this.obj = obj;
			this.key = key;
		}
	}

}

class InvalidClassException extends Exception {

    InvalidClassException() {
        super("Invalid class specification. This class is not loaded into the current Java runtime. " +
              "It might have been loaded into a different Java runtime, or Java might have been restarted " +
              "since the class was loaded.");
    }
}
