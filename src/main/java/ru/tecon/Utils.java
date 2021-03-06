package ru.tecon;

import ru.tecon.beanInterface.LoadOPCRemote;
import weblogic.jndi.WLInitialContextFactory;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.nio.file.Path;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Hashtable;

/**
 * Класс с сервисными функциями
 */
public class Utils {

    /**
     * Метод выгружает параметр удаленного сервера
     * @return строка подключения к удаленному серверу
     * @throws NamingException если возникла ошибка поиска переменной
     */
    private static String getRemoteServer() throws NamingException {
        Context ctx = new InitialContext();
        return (String) ctx.lookup("java:comp/env/remoteServer");
    }

    /**
     * Метод возвращает ejb bean класс для работы с базой
     * @return ejb класс
     * @throws NamingException ошибка
     */
    public static LoadOPCRemote loadRMI() throws NamingException {
        Hashtable<String, String> ht = new Hashtable<>();
        ht.put(Context.INITIAL_CONTEXT_FACTORY, WLInitialContextFactory.class.getName());
        ht.put(Context.PROVIDER_URL, getRemoteServer());

        Context ctx = new InitialContext(ht);

        return (LoadOPCRemote) ctx.lookup("ejb.LoadOPC#" + LoadOPCRemote.class.getName());
    }

    /**
     * Перевод байтов в читаемый вид (1024)
     * @param bytes количество байтов
     * @return читаемый вид
     */
    public static String humanReadableByteCountBin(long bytes) {
        long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        if (absB < 1024) {
            return bytes + " B";
        }
        long value = absB;
        CharacterIterator ci = new StringCharacterIterator("KMGTPE");
        for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
            value >>= 10;
            ci.next();
        }
        value *= Long.signum(bytes);
        return String.format("%.1f %cB", value / 1024.0, ci.current());
//        return String.format("%.1f %ciB", value / 1024.0, ci.current());
    }

    /**
     * Перевод байтов в читаемый вид (1000)
     * @param bytes количество байтов
     * @return читаемый вид
     */
    public static String humanReadableByteCountSI(long bytes) {
        if (-1000 < bytes && bytes < 1000) {
            return bytes + " B";
        }
        CharacterIterator ci = new StringCharacterIterator("kMGTPE");
        while (bytes <= -999_950 || bytes >= 999_950) {
            bytes /= 1000;
            ci.next();
        }
        return String.format("%.1f %cB", bytes / 1000.0, ci.current());
    }

    /**
     * Получаем расширение файла
     * @param path путь к файлу
     * @return расширение
     */
    public static String getExtension(Path path) {
        return getExtension(path.getFileName().toString());
    }

    /**
     * Получаем расширение файла
     * @param name имя файла
     * @return расширение
     */
    public static String getExtension(String name) {
        if (name.contains(".")) {
            return name.substring(name.lastIndexOf(".") + 1).toLowerCase();
        }
        return "";
    }
}
