/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-10-13
 * Mode: Test generation and verification.
 * Scope: Generated automated verification for the team-specified Reporting Service contracts.
 * Author review: I reviewed for correctness
 */
package sg.edu.nus.foc.reporting.report;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import sg.edu.nus.foc.reporting.error.BadRequestException;

import static org.junit.jupiter.api.Assertions.*;

class ImageValidatorTest {
    private final ImageValidator validator = new ImageValidator();

    @Test void acceptsZeroOneAndFivePhotosAndSanitizesUnsafeNames() {
        assertTrue(validator.validate(null).isEmpty());
        var one = validator.validate(List.of(png("../bad/na\u0000me?.png")));
        assertEquals("name_.png", one.getFirst().filename());
        assertEquals(5, validator.validate(List.of(png("1"), png("2"), png("3"), png("4"), png("5"))).size());
    }

    @Test void rejectsTooManyEmptyOversizedUnsupportedAndSignatureMismatch() {
        var six = new ArrayList<MockMultipartFile>(); for (int i = 0; i < 6; i++) six.add(png("p" + i));
        assertThrows(BadRequestException.class, () -> validator.validate(List.copyOf(six)));
        assertThrows(BadRequestException.class, () -> validator.validate(List.of(new MockMultipartFile("photos", "x.png", "image/png", new byte[0]))));
        assertThrows(BadRequestException.class, () -> validator.validate(List.of(new MockMultipartFile("photos", "x.png", "image/png", new byte[(int) ImageValidator.MAX_BYTES + 1]))));
        assertThrows(BadRequestException.class, () -> validator.validate(List.of(new MockMultipartFile("photos", "x.gif", "image/gif", new byte[]{1}))));
        assertThrows(BadRequestException.class, () -> validator.validate(List.of(new MockMultipartFile("photos", "x.png", "image/png", new byte[]{1, 2, 3}))));
    }

    @Test void validatesAllSupportedSignaturesAndFallbackNames() {
        byte[] jpeg = {(byte) 0xff, (byte) 0xd8, (byte) 0xff};
        byte[] webp = "RIFFxxxxWEBP".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        assertEquals("photo.jpg", validator.validate(List.of(new MockMultipartFile("photos", "..", "image/jpeg", jpeg))).getFirst().filename());
        assertEquals("photo.webp", validator.validate(List.of(new MockMultipartFile("photos", null, "image/webp", webp))).getFirst().filename());
        assertFalse(ImageValidator.matches("image/other", jpeg));
    }

    private static MockMultipartFile png(String name) {
        return new MockMultipartFile("photos", name, "image/png", new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});
    }
}
