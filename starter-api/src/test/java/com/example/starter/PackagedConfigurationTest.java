package com.example.starter;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

class PackagedConfigurationTest {
    private static final Pattern LOCAL_VALUE = Pattern.compile("localhost|127[.]0[.]0[.]1");
    private static final Pattern PLACEHOLDER_WITH_DEFAULT = Pattern.compile("[$][{][^}:]+:");

    @Test
    void holdsNoLocalValue() throws IOException {
        assertThat(offenders(LOCAL_VALUE)).isEmpty();
    }

    @Test
    void givesNoPlaceholderADefault() throws IOException {
        assertThat(offenders(PLACEHOLDER_WITH_DEFAULT)).isEmpty();
    }

    private static List<String> offenders(Pattern pattern) throws IOException {
        List<String> found = new ArrayList<>();
        for (PropertySource<?> source :
                new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yml"))) {
            Map<?, ?> values =
                    ((EnumerablePropertySource<?>) source).getSource() instanceof Map<?, ?> map ? map : Map.of();
            values.forEach((key, value) -> {
                if (pattern.matcher(String.valueOf(value)).find()) {
                    found.add(key + "=" + value);
                }
            });
        }
        return found;
    }
}
