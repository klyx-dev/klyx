package com.klyx.ktreesitter;

import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;

import java.io.File;

/**
 * The grammar configuration extension.
 */
public interface GrammarExtension {
    /**
     * The base directory of the grammar.
     *
     * <p>Default: {@code ../..}</p>
     */
    Property<File> getBaseDir();

    /**
     * The name of the grammar.
     *
     * <p><b>Required</b></p>
     */
    Property<String> getGrammarName();

    /**
     * The source files of the grammar.
     *
     * <p><b>Required</b></p>
     */
    Property<File[]> getFiles();

    /**
     * The include directories of the grammar.
     *
     * <p>Default: {@code ${baseDir}/bindings/c}</p>
     */
    Property<File[]> getIncludeDirs();

    /**
     * The include directories of the grammar.
     *
     * <p>Default: {@code tree-sitter-${grammarName}.h}</p>
     */
    Property<String[]> getIncludes();

    /**
     * The name of the C interop def file.
     *
     * <p>Default: {@code grammar}</p>
     */
    Property<String> getInteropName();

    /**
     * The name of the JNI library.
     *
     * <p>Default: {@code ktreesitter-${grammarName}}</p>
     */
    Property<String> getLibraryName();

    /**
     * The name of the package.
     *
     * <p><b>Required</b></p>
     */
    Property<String> getPackageName();

    /**
     * The name of the class.
     *
     * <p><b>Required</b></p>
     */
    Property<String> getClassName();

    /**
     * A map of Java methods to C functions.
     *
     * <p>Default: {@code language -> tree_sitter_${grammarName}}</p>
     */
    MapProperty<String, String> getLanguageMethods();
}
