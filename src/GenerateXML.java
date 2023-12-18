package mainClass;

import com.jayway.jsonpath.JsonPath;

import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.TestNG;
import org.testng.annotations.Test;
import org.testng.internal.ClassHelper;
import org.testng.internal.PackageUtils;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlInclude;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class GenerateXML {

	private static final int THREADS_COUNT = Integer.parseInt(System.getProperty("threads.count") == null ? "1" : System.getProperty("threads.count"));

    public static void main(String[] args) throws IOException, ClassNotFoundException {
       //you can add a method groups for specific methods across different classes or class groups to run entire classes
        String classGrps = System.getProperty("class.group") == null ? "" : System.getProperty("class.group");
        String methodGrps = System.getProperty("method.group") == null ? "" : System.getProperty("method.group");
        TestNG tng = new TestNG();
        List<XmlSuite> suites = new ArrayList<>();
        XmlSuite suite = new XmlSuite();
        suite.setName("TestNGSuite");
        suite.setParallel(XmlSuite.ParallelMode.TESTS);
        suite.setThreadCount(THREADS_COUNT);
        suite2.setParallel(XmlSuite.ParallelMode.TESTS);
        suite2.setThreadCount(THREADS_COUNT);
        List<String> listenerClasses = new ArrayList<>();
        listenerClasses.add("the target class name");
        suite.setListeners(listenerClasses);
        suite2.setListeners(listenerClasses);
        Map<String, String> params = new HashMap<>();
        //add test specific params
        params.put("environment", System.getProperty("local"));
        suite.setParameters(params);
        List<String> userNames = getUserNames(getTestMethods());
		boolean noGroups = classGrps.isEmpty() && methodGrps.isEmpty();
		boolean classGroups = !classGrps.isEmpty();
		boolean methodGroups = !methodGrps.isEmpty();
		if (classGroups || noGroups) {
			// class group
			List<String> classGroupsArray = Arrays.asList(classGrps.split(","));
			List<String> filteredClasses = filteredClassesArray(classGroupsArray, getTestMethods());
			List<String> classNames = (filteredClasses).stream()
					.filter(c -> !c.contains("TestConfigurationMethods")).collect(Collectors.toList());
			int count = 0;
			for (String className : classNames) {
				String[] testName = className.split("\\.");
				String testTag = testName[testName.length - 1];
				XmlTest test = new XmlTest(suite);
				Map<String, String> tparams = new HashMap<>();
                //class specific parameters
				tparams.put("username", System.getProperty(userNames.get(count)));
                test.setName(testTag);
                test.setParameters(tparams);
                XmlClass clazz = new XmlClass(className);
                test.setXmlClasses(Arrays.asList(clazz));
				}
				count++;
			}
		}
		if(methodGroups) {
			// Method grp
			List<String> methodGrpsArray = Arrays.asList(methodGrps.split(","));
			Map<String, String> methods = getTestMethodsFilteredByGroups(methodGrpsArray);
			List<String> testNames = new ArrayList<>();
			List<String> classNames = methods.values().stream().filter(r -> !r.contains("TestConfigurationMethods"))
					.collect(Collectors.toList());
			List<String> dclassNames = classNames.stream().distinct().collect(Collectors.toList());
			ListIterator<String> it = dclassNames.listIterator();
			int count = 0;
			while (it.hasNext()) {
				String[] testName = it.next().split("\\.");
				if (testName[testName.length - 1].contains("TestConfigurationMethods]")) {

				} else {
					testNames.add(testName[testName.length - 1]);
				}
			}
			for (String testName : testNames) {
				XmlTest test = new XmlTest(suite);
				test.setName(testName);
				Map<String, String> tparams = new HashMap<>();
				tparams.put("username", System.getProperty(userNames.get(count)));
				test.setParameters(tparams);
				XmlClass clazz = new XmlClass(classNames.stream().filter(c -> c.endsWith(testName)).findFirst().get());
				List<XmlInclude> classMethods = new ArrayList<>();
				methods.entrySet().stream().filter(r -> r.getValue().equals(clazz.getName()))
						.forEach(r -> classMethods.add(new XmlInclude(r.getKey())));
				clazz.setIncludedMethods(classMethods);
				test.setXmlClasses(Collections.singletonList(clazz));
				count++;

			}
		}
        suites.add(suite);
        //for debug purposes
        System.out.println(suite.toXml());
        tng.setXmlSuites(suites);
        //either add to resources to run as a mven project or just use tng.run();
        File myObj = new File("src/test/resources/"+suite.getName()+".xml");
        File myObj2 = new File("src/test/resources/"+suite2.getName()+".xml");
        FileWriter myWriter = new FileWriter(myObj);
        myWriter.write(suite.toXml());
        myWriter.close();
    }

    public static JSONObject getTestMethods() throws IOException, ClassNotFoundException {
        String[] testClasses =
                PackageUtils.findClassesInPackage("regressionTests.*", new
                        ArrayList<>(), new ArrayList<>());
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        JSONArray classesArray = new JSONArray();
        int i = 1;
        for (String eachClass : testClasses) {
            JSONArray methodsArray = new JSONArray();
            JSONObject jsonObject = new JSONObject();
            Class<?> currentClass = cl.loadClass(eachClass);
            jsonObject.put("ClassName", eachClass);
            try {
                jsonObject.put("ClassGroup", currentClass.getAnnotation(Test.class).groups());
            } catch (Exception e) {
                jsonObject.put("ClassGroup", "");
            }
            jsonObject.put("username", "user.name" + i);
            i++;
            if (i > THREADS_COUNT) {
                i = 1;
            }
            Set<Method> allMethods = ClassHelper.getAvailableMethods(currentClass);
            for (Method eachMethod : allMethods) {
                Test test;
                try {
                    test = eachMethod.getAnnotation(Test.class);
                    JSONObject jsonObject2 = new JSONObject();
                    jsonObject2.put("Name", eachMethod.getName());
                    jsonObject2.put("Groups", test.groups());
                    jsonObject2.put("Enabled", test.enabled());
                    methodsArray.put(jsonObject2);
                } catch (Exception ignored) {

                }
            }
            jsonObject.put("Methods", methodsArray);
            classesArray.put(jsonObject);
        }

        JSONObject clazz = new JSONObject();
        clazz.put("Classes", classesArray);
        return clazz;
    }

    public static List<String> filteredClassesArray(String groupName, JSONObject allClassesArray) {
        String path;
        if (groupName.isEmpty()) {
            path = "$[*]..ClassName";
        } else {
            path = String.format("$.[*]..[?(@.ClassGroup contains '%s')].ClassName", groupName);
        }

        String results = JsonPath.read(allClassesArray.toString(), path).toString();
        List<String> filteredResults = Arrays.asList(results.split(","));
        return filteredResults.stream().map(s -> s.replace("[", "").replace("\"", "").replace("]", "")).collect(Collectors.toList());
    }

	public static List<String> filteredClassesArray(List<String> groupNames, JSONObject allClassesArray) {
		String path;
		int groupsSize = groupNames.size();
		boolean emptyGroups = (groupsSize == 1 && groupNames.get(0).isEmpty());
		if (emptyGroups) {
			path = "$[*]..ClassName";
		} else {
			path = String.format("$.[*]..[?(@.ClassGroup contains '%s'", groupNames.get(0).trim());
			if (groupsSize > 1) {
				for (int i = 1; i < groupsSize; i++) {
					path = path + String.format("|| @.ClassGroup contains '%s'", groupNames.get(0).trim());
				}
			}
			path = path + ")].ClassName";
		}
		String results = JsonPath.read(allClassesArray.toString(), path).toString();
		List<String> filteredResults = Arrays.asList(results.split(","));
		return filteredResults.stream().map(s -> s.replace("[", "").replace("\"", "").replace("]", ""))
				.collect(Collectors.toList());
	}

    public static List<String> getUserNames(JSONObject allClassesArray) {
        String path = "$..username";
        String results = JsonPath.read(allClassesArray.toString(), path).toString();
        return Arrays.stream(results.split(",")).map(s -> s.replace("[", "").replace("\"", "")).collect(Collectors.toList());
    }

    public static Map<String, String> getTestMethodsFilteredByGroups(String groupName) throws IOException, ClassNotFoundException {
        String[] testClasses =
                PackageUtils.findClassesInPackage("regressionTests.*", new
                        ArrayList<>(), new ArrayList<>());
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Map<String, String> ftests = new HashMap<>();

        for (String eachClass : testClasses) {
            Class<?> currentClass = cl.loadClass(eachClass);
            Set<Method> allMethods = ClassHelper.getAvailableMethods(currentClass);
            for (Method eachMethod : allMethods) {
                try {
                    Test test = eachMethod.getAnnotation(Test.class);
                    if (Arrays.stream(test.groups()).anyMatch(s -> s.contains(groupName)))
                        ftests.put(eachMethod.getName(), eachClass);
                } catch (Exception ignored) {

                }
            }
        }
        return ftests;

    }

	public static Map<String, String> getTestMethodsFilteredByGroups(List<String> methodGrps)
			throws IOException, ClassNotFoundException {
		String[] testClasses = PackageUtils.findClassesInPackage("regressionTests.*", new ArrayList<>(),
				new ArrayList<>());
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		Map<String, String> ftests = new HashMap<>();
		for (String eachClass : testClasses) {
			Class<?> currentClass = cl.loadClass(eachClass);
			Set<Method> allMethods = ClassHelper.getAvailableMethods(currentClass);
			for (Method eachMethod : allMethods) {
				try {
					Test test = eachMethod.getAnnotation(Test.class);
					if (!test.toString().isEmpty()) {
						List<String> testGroups = Arrays.asList(test.groups());
						for (String grp : testGroups) {
							if (methodGrps.contains(grp)) {
								ftests.put(eachMethod.getName(), eachClass);
							}
						}
					}
				} catch (Exception ignored) {

				}
			}
		}
		return ftests;
	}
}
