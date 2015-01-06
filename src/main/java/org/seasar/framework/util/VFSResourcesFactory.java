package org.seasar.framework.util;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.jar.JarFile;

import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;
import org.seasar.framework.container.servlet.S2ContainerServletOnJBossAS7;
import org.seasar.framework.log.Logger;
import org.seasar.framework.util.ResourcesUtil.FileSystemResources;
import org.seasar.framework.util.ResourcesUtil.JarFileResources;
import org.seasar.framework.util.ResourcesUtil.Resources;
import org.seasar.framework.util.ResourcesUtil.ResourcesFactory;

/**
 * ResourcesFactory for JBossAS 7 vfs.
 *
 * @author wadahiro
 */
public class VFSResourcesFactory implements ResourcesFactory {

    private static final Logger logger = Logger
            .getLogger(S2ContainerServletOnJBossAS7.class);

    public Resources create(final URL url, final String rootPackage,
            final String rootDir) {

        debugArgs(url, rootPackage, rootDir);

        try {
            // convert vfs to file protocol
            VirtualFile vf = VFS.getChild(url.toURI());
            URI physicalURI = VFSUtils.getPhysicalURI(vf);

            debugPhysicalURI(physicalURI);
            if (url.getPath().toLowerCase().matches(".*\\.jar.*")) {

                int afterJarIndex = 0;

                if (null == rootPackage || "".equals(rootPackage)) {
                    afterJarIndex = url.getPath().lastIndexOf("/");
                } else {
                    afterJarIndex = url.getPath().toLowerCase().indexOf("/" + rootPackage.replaceAll("\\.", "/"));
                }

                String jarFilePath = url.getPath().substring(0, afterJarIndex);
                File file = new File(jarFilePath);
                String jarFileName = file.getName();

                //.jarを含む場合、jarをハンドリングする
                return handleJar(jarFileName, physicalURI, rootPackage, rootDir);

            } else {
                return handleDir(physicalURI, rootPackage, rootDir);
            }
        } catch (Exception e) {
            throw new RuntimeException("create resource error. url:" + url
                    + ", rootPackage:" + rootPackage + ", rootDir:" + rootDir,
                    e);
        }
    }

    /**
     * ディレクトリリソースをハンドリングします。
     *
     * @param physicalURI physicalURI
     * @param rootPackage rootPackage
     * @param rootDir rootDir
     * @return Resource
     * @throws MalformedURLException
     */
    private Resources handleDir(URI physicalURI, final String rootPackage,
            final String rootDir) throws MalformedURLException {
        return new FileSystemResources(ResourcesUtil.getBaseDir(
                physicalURI.toURL(), rootDir), rootPackage, rootDir);
    }

    /**
     * Jarファイルリソースをハンドリングします。
     *
     * @param jarFileName jarFileName
     * @param physicalURI physicalURI
     * @param rootPackage rootPackage
     * @param rootDir rootDir
     * @return Resource
     * @throws IOException
     */
    private Resources handleJar(String jarFileName, URI physicalURI, final String rootPackage,
            final String rootDir) throws IOException {


        String physicalURIStr = physicalURI.getPath();
        physicalURIStr = physicalURIStr.replaceAll("file:", "");
        int strIndex = physicalURIStr.lastIndexOf("/contents");
        physicalURIStr = physicalURIStr.substring(0, strIndex + 1);
        physicalURIStr += jarFileName;


        File f = new File(physicalURIStr);

        File[] jar = f.getParentFile().listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.getName().toLowerCase().endsWith(".jar");
            }
        });

        debugJarPath(jar);
        JarFile jarFile = new JarFile(jar[0]);
        return new JarFileResources(jarFile, rootPackage, rootDir);
    }

    /**
     * PhysicalURI情報をデバッグログ出力します。
     *
     * @param physicalURI PhysicalURI
     */
    private void debugPhysicalURI(URI physicalURI) {
        if (logger.isDebugEnabled()) {
            logger.debug("Converted physicalURI:" + physicalURI);
        }
    }

    /**
     * Jarファイルパスをデバッグログ出力します。
     *
     * @param jarFile jarFile
     */
    private void debugJarPath(File[] jarFile) {
        if (logger.isDebugEnabled()) {
            for (File file : jarFile) {
                logger.debug("handle jar. path:" + file.getAbsolutePath());
            }
        }
    }

    /**
     * 引数の情報をデバッグログ出力します。
     *
     * @param url URL
     * @param rootPackage RootPackage
     * @param rootDir RootDir
     */
    private void debugArgs(URL url, String rootPackage, String rootDir) {
        if (logger.isDebugEnabled()) {
            logger.debug("create resource: dump args...");
            logger.debug("--> url:" + url);
            logger.debug("--> rootPackage:" + rootPackage);
            logger.debug("--> rootDir:" + rootPackage);
        }
    }

}