package com.localroots.clientfiles.attachment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DownloadFileNameBuilderTest {

    @Test
    void usesDisplayNameAndPreservesOriginalExtension() {
        assertEquals(
                "bobs_lawn.png",
                DownloadFileNameBuilder.build(
                        "bob's lawn",
                        "local_roots_logo_facebook_cover.png",
                        "image/png"
                )
        );
    }

    @Test
    void doesNotAppendTheExtensionTwice() {
        assertEquals(
                "Front_Yard.jpg",
                DownloadFileNameBuilder.build(
                        "Front Yard.JPG",
                        "IMG_1234.JPG",
                        "image/jpeg"
                )
        );
    }

    @Test
    void infersExtensionFromContentTypeWhenOriginalNameHasNone() {
        assertEquals(
                "Estimate_July.pdf",
                DownloadFileNameBuilder.build(
                        "Estimate July",
                        "estimate",
                        "application/pdf"
                )
        );
    }

    @Test
    void fallsBackToOriginalNameWhenDisplayNameIsBlank() {
        assertEquals(
                "estimate.pdf",
                DownloadFileNameBuilder.build(
                        " ",
                        "estimate.PDF",
                        "application/pdf"
                )
        );
    }

    @Test
    void addsTxtExtensionForPlainTextWhenOriginalNameHasNoExtension() {
        assertEquals(
                "Estimate_-_Todd_Williamson.txt",
                DownloadFileNameBuilder.build(
                        "Estimate - Todd Williamson",
                        "estimate",
                        "text/plain; charset=UTF-8"
                )
        );
    }

}
