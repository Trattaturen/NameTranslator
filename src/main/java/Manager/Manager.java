package Manager;

import java.util.List;

public interface Manager
{
    void submitTranslation(List<String> origins, List<String> translatinos);
    List<String> getNextWord();
}