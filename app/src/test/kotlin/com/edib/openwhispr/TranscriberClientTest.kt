package com.edib.openwhispr

import org.junit.Assert.*
import org.junit.Test

class TranscriberClientTest {

    @Test fun `parses success response`() {
        val r = TranscriberClient.parseResponse("""{"text": "Hello world"}""")
        assertEquals("Hello world", r.text)
        assertNull(r.error)
    }

    @Test fun `discards transcription with strong silence metadata`() {
        val raw = """{"text":"Tyskland, Tyskland",
           "segments":[{"text":"Tyskland","no_speech_prob":0.98,"avg_logprob":-1.5}]}"""
        val result = TranscriberClient.parseResponse(raw)
        assertEquals("", result.text)
    }

    @Test fun `keeps real speech even if some segments are silent`() {
        val raw = """{"text":"Hi I like granola bars.",
           "segments":[{"text":"Hi I like granola bars","no_speech_prob":0.03,"avg_logprob":-0.2},
           {"text":"","no_speech_prob":0.97,"avg_logprob":-1.2}]}"""
        val result = TranscriberClient.parseResponse(raw)
        assertEquals("Hi I like granola bars.", result.text)
    }

    @Test fun `parses error response`() {
        val r = TranscriberClient.parseResponse("""{"error":{"message":"Invalid key","type":"auth"}}""")
        assertNull(r.text)
        assertEquals("Invalid key", r.error)
    }

    @Test fun `handles unknown format`() {
        val r = TranscriberClient.parseResponse("""{"foo":"bar"}""")
        assertNull(r.text)
        assertNotNull(r.error)
    }

    @Test fun `handles malformed json`() {
        val r = TranscriberClient.parseResponse("not json")
        assertNull(r.text)
        assertNotNull(r.error)
    }

    @Test fun `builds Groq Whisper multipart with dictionary-only prompt`() {
        val request = TranscriberClient.transcriptionRequest(
            byteArrayOf(1, 2, 3),
            "test-key",
            listOf("OpenWhispr | open whisper, open wisper", "NASA"),
        )
        val multipart = request.body as okhttp3.MultipartBody
        val parts = multipart.parts
        assertEquals("verbose_json", partContent(parts.first { it.hasDispositionName("response_format") }))
        val model = partContent(parts.first { it.hasDispositionName("model") })
        val prompt = partContent(parts.first { it.hasDispositionName("prompt") })

        assertEquals("https://api.groq.com/openai/v1/audio/transcriptions", request.url.toString())
        assertEquals("whisper-large-v3", model)
        assertTrue(prompt.contains("OpenWhispr"))
        assertTrue(prompt.contains("open whisper"))
        assertTrue(prompt.contains("open wisper"))
        assertTrue(prompt.contains("NASA"))
        assertFalse(prompt.contains("cleanup"))
        assertTrue(prompt.toByteArray(Charsets.UTF_8).size <= TranscriberClient.MAX_DICTIONARY_PROMPT_BYTES)
    }

    @Test fun `omits Groq prompt multipart field when dictionary is empty`() {
        val request = TranscriberClient.transcriptionRequest(byteArrayOf(1, 2, 3), "test-key", emptyList())
        val multipart = request.body as okhttp3.MultipartBody

        assertEquals("whisper-large-v3", partContent(multipart.parts.first { it.hasDispositionName("model") }))
        assertFalse(multipart.parts.any { it.hasDispositionName("prompt") })
    }

    @Test fun `bounds UTF-8 dictionary prompt below Groq token limit`() {
        val prompt = TranscriberClient.dictionaryPrompt(listOf("Élodie", "界".repeat(100)))

        assertTrue(prompt.toByteArray(Charsets.UTF_8).size <= 128)
        assertTrue(prompt.contains("Élodie"))
    }

    private fun partContent(part: okhttp3.MultipartBody.Part): String =
        okio.Buffer().also { part.body.writeTo(it) }.readUtf8()

    private fun okhttp3.MultipartBody.Part.hasDispositionName(name: String): Boolean =
        headers?.get("Content-Disposition")?.contains("name=\"$name\"") == true

}
