package telegram.bot.Configuration;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class TokenBlackList {
    private Set<String> blacklist = new HashSet<>();

    public void add(String token) {

        blacklist.add(token);
    }

    public boolean isBlacklisted(String token) {

        return blacklist.contains(token);
    }
}

