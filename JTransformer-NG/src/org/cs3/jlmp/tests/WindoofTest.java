package org.cs3.jlmp.tests;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.cs3.jlmp.JLMP;
import org.cs3.jlmp.JLMPPlugin;
import org.cs3.pl.common.Debug;
import org.cs3.pl.common.ResourceFileLocator;
import org.cs3.pl.common.Util;
import org.cs3.pl.prolog.PrologException;
import org.cs3.pl.prolog.PrologSession;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.text.BadLocationException;

/**
 * Testing for bytecode invariance of fact generation / source re-generation
 * roundtripp.
 * 
 * 
 * <p>
 * This testcase will
 * <ul>
 * <li>setup the converter testproject</li>
 * <li>traverse all packeges starting with "test"</li>
 * <li>for each package
 * <ul>
 * <li>create prolog facts for all files in this package.</li>
 * <li>consult the generated facts</li>
 * <li>normalize all source files in that package</li>
 * <li>compile all files in the package</li>
 * <li>rename the resulting class files by attaching the prefix ".orig" This
 * set of files is until now adressed as "the original bytecode"</li>
 * <li>rename the normalized source files by attaching the prefix ".orig" Those
 * files will be adressed as "the original source code"</li>
 * <li>regenerate the source code of all toplevels present in the prolog system
 * </li>
 * <li>normalize the resulting source files in the package. These files will
 * from now on be called "the generated sourcecode"</li>
 * <li>Assert that for each original source file there is a generated source
 * file with corresponding name.</li>
 * <li>Assert that for each generated source file there is an original source
 * file with corresponding name.</li>
 * <li>compile all files in the package, from now on adressed as "the generated
 * bytecode"</li>
 * <li>assert that for each original bytecode file there is a generated
 * bytecode file with corresponding name.</li>
 * <li>assert that for each generated bytecode file there is an original
 * bytecode file with corresponding name.</li>
 * <li>assert that each corresponding pair of original and generated bytecode
 * files is binary identical.</li>
 * </ul>
 * </li>
 * </ul>
 *  
 */
public class WindoofTest extends FactGenerationTest {

    private final class Comparator implements IResourceVisitor {
        public boolean visit(IResource resource) throws CoreException {
            switch (resource.getType()) {
            case IResource.FOLDER:
                return true;
            case IResource.FILE:
                IFile file = (IFile) resource;
                if (!file.getFileExtension().equals("class"))
                    return false;

                IFile orig = ResourcesPlugin.getWorkspace().getRoot().getFile(
                        file.getFullPath().addFileExtension("orig"));
                assertTrue(packageName
                        + ": original class file not accessible: "
                        + orig.getFullPath().toString(), orig.isAccessible());
                //both files should be of EXACTLY the same size:
                BufferedReader origReader = new BufferedReader(
                        new InputStreamReader(orig.getContents()));
                BufferedReader genReader = new BufferedReader(
                        new InputStreamReader(file.getContents()));
                int origR = 0;
                int genR = 0;
                int i = 0;
                for (i = 0; origR != -1 && genR != -1; i++) {
                    try {
                        origR = origReader.read();
                        genR = genReader.read();
                        assertTrue(
                                packageName
                                        + ": orig and generated file differ at position "
                                        + i + ": " + orig.getName(),
                                origR == genR);
                    } catch (IOException e) {
                        org.cs3.pl.common.Debug.report(e);
                    }
                }
                org.cs3.pl.common.Debug.info("compared " + i
                        + " chars succsessfully.");
                return false;

            }
            return false;
        }
    }

    private final class Renamer implements IResourceVisitor {
        String[] extensions = null;

        String suffix = null;

        public Renamer(String extensions[], String suffix) {
            this.extensions = extensions;
            this.suffix = suffix;
        }

        public boolean visit(IResource resource) throws CoreException {
            switch (resource.getType()) {
            case IResource.FOLDER:
                return true;
            case IResource.FILE:
                IFile file = (IFile) resource;
                if (!file.isAccessible()) {
                    Debug.warning("RENAMER:not accsessible: "
                            + file.getFullPath());
                    break;
                }
                if (extensions == null || extensions.length == 0) {
                    file.move(file.getFullPath().addFileExtension(suffix),
                            true, null);
                    break;
                }
                for (int i = 0; i < extensions.length; i++) {
                    if (extensions[i].equals(file.getFileExtension())) {

                        try {
                            file.move(file.getFullPath().addFileExtension(
                                    suffix), true, null);
                        } catch (Throwable t) {
                            Debug.report(t);
                        }

                        break;
                    }
                }
                break;
            case IResource.PROJECT:
                return true;
            default:
                throw new IllegalStateException("Unexpected resource type.");
            }
            return false;
        }
    }

    private String packageName;

    private boolean passed;

    /**
     * @param name
     */
    public WindoofTest(String name) {
        super(name);
        this.packageName = name;
    }

    /**
     * @param string
     * @param string2
     */
    public WindoofTest(String name, String packageName) {
        super(name);

        this.packageName = packageName;
    }

    protected Object getKey() {

        return WindoofTest.class;
    }

    public void setUpOnce() throws Exception {
        super.setUpOnce();

        //install test workspace
        ResourceFileLocator l = JLMPPlugin.getDefault().getResourceLocator("");
        File r = l.resolve("testdata-roundtrip.zip");
        Util.unzip(r);
        org.cs3.pl.common.Debug.info("setUpOnce caled for key  " + getKey());
        setAutoBuilding(false);

    }

    public void testIt() throws CoreException, IOException,
            BadLocationException, InterruptedException {
        testIt_impl();
        passed = true;

    }

    public synchronized void testIt_impl() throws CoreException, IOException,
            BadLocationException, InterruptedException {

        Util.startTime("untilBuild");
        IProject project = getTestProject();
        IJavaProject javaProject = getTestJavaProject();

        org.cs3.pl.common.Debug.info("Running (Pseudo)roundtrip in "
                + packageName);
        //retrieve all cus in package
        ICompilationUnit[] cus = getCompilationUnitsInFolder(packageName);
        //normalize source files
        normalize(cus);
        IFolder folder = project.getFolder(packageName);
        IFile javaFile = folder.getFile("Test.java");
        assertTrue(javaFile.isSynchronized(IResource.DEPTH_INFINITE));
        assertTrue(javaFile.exists());
        build(JavaCore.BUILDER_ID);

        //Thread.sleep(1000);
        IFile classFile = folder.getFile("Test.class");
        assertTrue(classFile.isSynchronized(IResource.DEPTH_INFINITE));
        assertTrue(classFile.exists());
        build(JLMP.BUILDER_ID);

        rename(folder, new String[] { "java", "class" }, "orig");
        assertTrue(classFile.isSynchronized(IResource.DEPTH_INFINITE));
        assertFalse(classFile.exists());
        assertTrue(javaFile.isSynchronized(IResource.DEPTH_INFINITE));
        assertFalse(javaFile.exists());
        classFile = folder.getFile("Test.class.orig");
        javaFile = folder.getFile("Test.java.orig");
        assertTrue(classFile.isSynchronized(IResource.DEPTH_INFINITE));
        assertTrue(classFile.exists());
        assertTrue(javaFile.isSynchronized(IResource.DEPTH_INFINITE));
        assertTrue(javaFile.exists());

        generateSource();
        /*
         * javaFile = folder.getFile("Test.java");
         * assertTrue(javaFile.isSynchronized(IResource.DEPTH_INFINITE));
         * assertTrue(javaFile.exists());
         * 
         * build(JavaCore.BUILDER_ID); classFile = folder.getFile("Test.class");
         * assertTrue(classFile.isSynchronized(IResource.DEPTH_INFINITE));
         * assertTrue(classFile.exists());
         */

    }

    protected synchronized void tearDown() throws Exception {
        uninstall(packageName);
        IFolder folder = getTestProject().getFolder(packageName);
        assertTrue(folder.isSynchronized(IResource.DEPTH_INFINITE));
        assertFalse(folder.exists());
    }

    /**
     * @param cus
     * @throws CoreException
     */
    private void normalize(final ICompilationUnit[] cus) throws CoreException {
        IWorkspaceRunnable r = new IWorkspaceRunnable() {
            public void run(IProgressMonitor mannomonDaRocktDerHase) {
                for (int i = 0; i < cus.length; i++) {
                    ICompilationUnit cu = cus[i];

                    try {
                        normalizeCompilationUnit(cu);
                    } catch (Exception e) {
                        throw new RuntimeException(packageName
                                + ": could not normalize cu "
                                + cu.getElementName(), e);
                    }
                }
            }
        };
        ResourcesPlugin.getWorkspace().run(r, getTestProject(),
                IWorkspace.AVOID_UPDATE, null);
    }

    /**
     * @param folder
     * @throws CoreException
     */
    private void compare(final IResource folder) throws CoreException {
        final IResourceVisitor comparator = new Comparator();
        IWorkspaceRunnable r = new IWorkspaceRunnable() {
            public void run(IProgressMonitor monitor) throws CoreException {
                try {
                    folder.accept(comparator);
                } catch (Throwable e) {
                    Debug.report(e);
                    throw new RuntimeException(e);
                }
            }
        };
        ResourcesPlugin.getWorkspace().run(r, getTestProject(),
                IWorkspace.AVOID_UPDATE, null);
    }

    /**
     * @param folder
     * @throws CoreException
     */
    private void rename(final IResource root, String[] exts, String suffix)
            throws CoreException {
        final IResourceVisitor renamer = new Renamer(exts, suffix);
        IWorkspaceRunnable r = new IWorkspaceRunnable() {
            public void run(IProgressMonitor monitor) throws CoreException {
                try {
                    root.accept(renamer);
                    root.refreshLocal(IResource.DEPTH_INFINITE, null);
                } catch (Throwable e) {
                    Debug.report(e);
                    throw new RuntimeException(e);
                }
            }
        };
        ResourcesPlugin.getWorkspace().run(r, getTestProject(),
                IWorkspace.AVOID_UPDATE, null);
    }

    protected synchronized void setUp() throws Exception {
        super.setUp();
        setTestDataLocator(JLMPPlugin.getDefault().getResourceLocator(
                "testdata-roundtrip"));

        install(packageName);
        passed = false;

    }

    public void tearDownOnce() {
        super.tearDownOnce();
    }

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#getName()
     */
    public String getName() {
        return packageName;
    }

    public static Test suite() {
        TestSuite s = new TestSuite();
        BitSet blacklist = new BitSet();

       s.setName("WindoofTest");
        

        for (int i = 1; i <= 10; i++) {//1-539
            if (!blacklist.get(i)) {
                s.addTest(new WindoofTest("testIt", generatePackageName(i)));
            }
        }
        return s;
    }

    /**
     * @param i
     * @return
     */
    private static String generatePackageName(int n) {
        int desiredLength = 4;
        String number = String.valueOf(n);
        int padLength = desiredLength - number.length();
        StringBuffer sb = new StringBuffer("test");
        for (int i = 0; i < padLength; i++)
            sb.append('0');
        sb.append(number);
        return sb.toString();
    }

}