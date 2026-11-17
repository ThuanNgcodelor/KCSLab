package com.example.app.results.service;

/** Exposes small fixture builders to other test packages. */
public final class ResultFilePolicyTestSupport {
    private ResultFilePolicyTestSupport() {}

    public static byte[] docx() throws Exception { return ResultFilePolicyTest.docx(); }
}
