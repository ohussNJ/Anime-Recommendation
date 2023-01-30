package com.example.animerec;

import java.io.IOException;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class AnimeRec {
    int numberOfEpisodes=0;              // Maximum number of episodes recommended show can have
    String genre="";                     // Genre of recommendation
    String showName="";                  // Original show user wants a recommendation based on
    float minScore=0;                    // Minimum score for recommended show
    Boolean recommendationFound=false;   // Checks if a show recommendation was given
    WebDriver wd=startWebDriver();

    // Extract initial information from user for web search
    public void getUserInfo(){
        List<String> listOfGenres = Arrays.asList("action", "adventure" , "comedy", "drama", "fantasy", "horror",
                "mystery", "psychological", "romance");
        Scanner scan=new Scanner(System.in);
        System.out.println("Welcome!");
        System.out.println("Please answer a few questions so an anime recommendation can be given.");
        System.out.println("Enter the name of the anime show that you would like to receive recommendations based on: ");
        showName= scan.nextLine();
        System.out.println("What is the maximum number of episodes for the series (minimum 12): ");
        numberOfEpisodes= scan.nextInt();
        while (numberOfEpisodes<12){
            System.out.println("Error please choose a number above or equal 12: ");
            numberOfEpisodes= scan.nextInt();
        }
        System.out.println("Please select a genre from the list: ");
        System.out.println(listOfGenres);
        genre= scan.next();
        while (!listOfGenres.contains(genre)){
            System.out.println("Error choose an option from the list: ");
            System.out.println(listOfGenres);
            genre= scan.next();
        }
        genre = genre.substring(0,1).toUpperCase() + genre.substring(1);
        System.out.println("Please enter a minimum score the anime must have (1-10)(Note: A high score may not yield results): ");
        minScore = scan.nextFloat();
        while (minScore <1 || minScore>10){
            System.out.println("Error score must be between 1 and 10: ");
            minScore = scan.nextInt();
        }
        String animeSearch = findShowBackgroundInfo(showName);
        navigateWebPage(animeSearch);
        scan.close();
    }

    // Start webdriver with necessary properties
    public WebDriver startWebDriver(){
        //System.setProperty("webdriver.chrome.logfile", "C:\\Users\\ohuss\\Downloads\\chromedriverchromedriver.log");
        //System.setProperty("webdriver.chrome.driver", "C:\\Users\\ohuss\\Downloads\\chromedriverchromedriver.exe");
        System.setProperty("webdriver.chrome.silentOutput","true");
        System.setProperty("webdriver.chrome.driver","C:\\Users\\ohuss\\Downloads\\chromedriver.exe");
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--log-level=3");
        WebDriver wd = new ChromeDriver(options);         //create an instance for webdriver named wd of type chrome
        return wd;
    }

    // Use selenium for initial search in order to start extracting information about user's initial show
    public String findShowBackgroundInfo(String userShow) {
        wd.get("https://myanimelist.net/anime.php");
        WebElement elem = wd.findElement(By.name("q"));     //finding the web element using name locator
        elem.sendKeys(new String[]{userShow});
        elem.submit();
        return wd.getCurrentUrl();
    }

    // Use Jsoup to find anime web profile
    public void navigateWebPage(String url){
        String animeWebProfile="";
            System.out.println();
            System.out.println("Extracting anime web profile...");
        try {
            Document doc = Jsoup.connect(url).get();
            Elements body = doc.select("tbody");
            ArrayList<String> hre=new ArrayList<>();
            int j = 0;
            for (Element e : body.select("tr")) {
                hre.add(j, e.select("a").attr("href").toString());
                j++;
            }
            for (String s:hre){
                if (s.contains("http")){
                    animeWebProfile=s;
                    break;
                }
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
        createAnimeList(animeWebProfile);
    }

    // Create a list of anime that may be similar to the user's show (same director, studio, themes)
    public void createAnimeList(String url){
        String userRecPage = "";
        ArrayList<String> links=new ArrayList<String>();
        System.out.println();
        System.out.println("Creating list of possible recommendations...");
        System.out.println();
        try {
            Document doc = Jsoup.connect(url).get();
            Elements body = doc.select("tbody");
            userRecPage = doc.getElementsByClass("btn-detail-recommendations-view-all fl-r").attr("href").toString();
            Document rec = Jsoup.connect(userRecPage).get();
            Elements recBody = rec.select("tbody");
            int i = 0;
            for (Element x : recBody.select("tbody")) {
                String link=x.select("a").attr("href").toString();
                i++;
                if (i>2)
                    links.add(link);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        getShowInfo(links);
    }

    // Retrieve information (episodes, rating, genres) for each show in the list of possible recommendations
    public void getShowInfo(ArrayList<String> showList){
        HashMap<String, ArrayList<String>> showListInfo=new HashMap<>();
        ArrayList<String> showAttributes=new ArrayList<>();
        int i=0;
        for (String show: showList){
            i++;
            if (i==50) break;
            try {
                Document doc = Jsoup.connect(show).get();
                Elements body = doc.select("tbody");
                String fullName=doc.getElementsByClass("spaceit_pad").text().toString();
                String name= retrieveName(fullName);
                showListInfo.put(name, showAttributes(name, show));
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        retrieveTopShows(showListInfo);
    }

    // Helper method to extract name of a possible show recommendation
    public String retrieveName(String fullName){
        String [] nameSeperated=fullName.split(" ");
        String nameRetrieved="";
        for (int i=1; i<10; i++) {
            if (nameSeperated[i].equals("Japanese:") || nameSeperated[i].equals("Synonyms:") ||
            nameSeperated[i].equals("Main") || nameSeperated[i].equals("Supporting") ||
                    nameSeperated[i].equals("Type:"))
                break;
            nameRetrieved += nameSeperated[i] + " ";
        }
        return nameRetrieved.trim();
    }

    // Gather ratings, episode count, ang genre list for each possible recommendation
    public ArrayList<String> showAttributes(String show, String animePage){
        ArrayList<String> showInfo=new ArrayList<>();
        try {
            Document doc = Jsoup.connect(animePage).get();
            Elements body = doc.select("tbody");
            String episodes=doc.select("div:containsOwn(Episodes:)").text();
            episodes=checkEpisodes(episodes);
            String rating=doc.getElementsByClass("spaceit_pad po-r js-statistics-info di-ib").text().toString();
            rating= rating.substring(7,11);
            ArrayList <String> showGenres=new ArrayList<>();
            Element e = body.select("div:contains(Genres:)").get(1);
            showGenres=checkGenres(e.text().toString());
            showInfo.add(episodes);
            showInfo.add(rating);
            System.out.println(show);
            for (String sGenre: showGenres) {
                showInfo.add(sGenre);
            }
        }   catch (Exception ignore) {
        }
        return showInfo;
    }

    // Helper method to check the number of episodes
    public String checkEpisodes(String num){
        String [] splitEpisodes=num.split(":");
        num=splitEpisodes[1];
        if (num.contains("/"))
                num=num.substring(2,num.length());
        return num;
    }

    // Creates list of genres for each show
    public ArrayList <String> checkGenres(String gList){
        ArrayList <String> showGenres=new ArrayList<>();
        String [] genreSeparated=gList.split(",");
        int i=0;
        for (String diffGenre:genreSeparated){
            int length=diffGenre.length()+1;
            if (i==0) {
                length += 7;
                diffGenre=diffGenre.substring(8,length/2);
            }
            if (i>=1)
                diffGenre=diffGenre.substring(1, length/2);
            showGenres.add(diffGenre);
            i++;
        }
        return showGenres;
    }

    // Final retrieval of shows based on user preference
    public void retrieveTopShows(HashMap<String, ArrayList<String>> showListInfo){
        System.out.println();
        System.out.println("Narrowing results based on preference...");
        Iterator it = showListInfo.keySet().iterator();
        while (it.hasNext()) {
            String keyShow = it.next().toString();
            ArrayList<String> showCheck = showListInfo.get(keyShow);
            if (showCheck.size()==0){
                it.remove();
                continue;
            }
            if (!Character.isDigit(showCheck.get(0).charAt(0)) || !Character.isDigit(showCheck.get(1).charAt(0)) ||
                    showCheck.get(0).length()>4 || showCheck.get(1).length()>4) {
                it.remove();
                continue;
            }
            int showLength=Integer.valueOf(showCheck.get(0));
            float showRating=Float.valueOf(showCheck.get(1));
            if (showLength > numberOfEpisodes || showRating < minScore || !showCheck.contains(genre)) {
                    it.remove();
                    continue;
                }
        }
        int i=1;
        if (!showListInfo.isEmpty()) {
            recommendationFound=true;
            System.out.println();
            System.out.println("Done! Here is a list of shows according to your preferences:");
            System.out.println();
            for (String keyShow : showListInfo.keySet()) {
                ArrayList<String> showCheck = showListInfo.get(keyShow);
                System.out.println(i + ". " + keyShow + " (" + showCheck.get(0) + " episodes, " + showCheck.get(1) + " rating)");
                i++;
            }
        }
        System.out.println();
    }

    public String toString(){
        if (recommendationFound)  return "Enjoy!";
        else return "No show was found. Try to refine your search for better results.";
    }

    public static void main(String[] args) {
        AnimeRec user=new AnimeRec();
        user.getUserInfo();
        System.out.println(user);
    }
}
