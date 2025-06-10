package org.docpirates.ispi.service;

import org.springframework.stereotype.Service;

@Service
public class ContactInfoService {
    public static boolean containsContactInfo(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase();
        String normalized = lower.replaceAll("[^a-zа-яіїєґ0-9]", "");
        String[] keywords = {
                "tg", "telegram", "telega", "тлг", "теле", "теграм", "телеґрам",
                "inst", "instagram", "інст", "інстаграм",
                "нік", "username", "акаунт", "аккаунт", "gmail", "yahoo", "hotmail",
                "outlook", "ukrnet", "i.ua", "meta.ua", "mail.ru", "protonmail"
        };
        for (String keyword : keywords) {
            if (normalized.contains(keyword))
                return true;
        }

        if (text.contains("@") || text.contains("_"))
            return true;

        if (text.matches("(?i).*https?://.*") || text.matches(".*\\.(com|ua|net|org|me|gg|io)(\\W|$).*"))
            return true;

        if (text.matches(".*\\+?\\d{1,3}[- ()]*\\d{2,3}[- ()]*\\d{2,3}[- ()]*\\d{2,4}.*"))
            return true;

        if (text.matches(".*[a-zA-Z0-9._%+-]+\\s*@\\s*[a-zA-Z0-9.-]+\\s*\\.\\s*[a-zA-Z]{2,}.*"))
            return true;
        return false;
    }

}
