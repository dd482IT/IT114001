

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.time.temporal.WeekFields;
import java.util.Random;
import java.util.Scanner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;

public class NumberGuesserHW {
    private int level = 1;
    private int strikes = 0;
    private int maxStrikes = 5;
    private int number = 0;
    private int win = 0;//added
    private int lose = 0;//added
    private float WLratio = 0f; //added
    private boolean savedNum = false;//added
    private boolean isRunning = false;
    final String saveFile = "numberGuesserSave.txt";

    /***
     * Gets a random number between 1 and level.
     *
     * @param level (level to use as upper bounds)
     * @return number between bounds
     */
//----------------------------------------------------------
    public static int getNumber(int level) {
        int range = 9 + ((level - 1) * 5);
        System.out.println("I picked a random number between 1-" + (range + 1) + ", let's see if you can guess.");
        return new Random().nextInt(range) + 1;
    }

    public static void getRange(int level){
        int range = 9 + ((level - 1) * 5);
        System.out.println("I picked a random number between 1-" + (range + 1) + ", let's see if you can guess.");
    }
    //----------------------------------------------------------
    private void win() {
        System.out.println("That's right!");
        level++;// level up!
        win++;//added
        System.out.println("Your current WL ratio is " + (WLratio()));
        saveLevel();//added
        strikes = 0;
        System.out.println("Welcome to level " + level);
        number = getNumber(level);
    }

    private float WLratio(){//added
        if(lose == 0)
            return 0;
        else {
            WLratio = win / lose;
            return WLratio;
        }
    }

    //----------------------------------------------------------
    private void lose() {
        System.out.println("Uh oh, looks like you need to get some more practice.");
        System.out.println("The correct number was " + number);
        strikes = 0;
        level--;
        lose++;//added
        System.out.println("Your current WL ratio is " + (WLratio()));
        if (level < 1) {
            level = 1;
        }
        saveLevel();
        number = getNumber(level);
    }
    //----------------------------------------------------------
    private void processCommands(String message) {
        if (message.equalsIgnoreCase("quit")) {
            System.out.println("Tired of playing? No problem, see you next time.");
            isRunning = false;
        }
    }
    //----------------------------------------------------------
    private void processGuess(int guess)
    {
        if (guess < 0) {
            return;
        }
        System.out.println("You guessed " + guess);
        if (guess == number) {
            win();
            saveLevel();//added
        } else {
            System.out.println("That's wrong");
            strikes++;
            if (strikes >= maxStrikes) {
                saveLevel();//added
                lose();
            } else {
                int remainder = maxStrikes - strikes;
                saveLevel();//added
                System.out.println("You have " + remainder + "/" + maxStrikes + " attempts remaining");
                if (guess > number) {
                    System.out.println("Lower");
                } else if (guess < number) {
                    System.out.println("Higher");
                }
            }
        }
    }
    //----------------------------------------------------------
    private int getGuess(String message) {
        int guess = -1;
        try {
            guess = Integer.parseInt(message);
        } catch (NumberFormatException e) {
            System.out.println("You didn't enter a number, please try again");

        }
        saveLevel(); //added
        return guess;
    }
    //----------------------------------------------------------
    private void saveLevel() {
        try (FileWriter fw = new FileWriter(saveFile)) {
            fw.write("" + level);// here we need to convert it to a String to record correctly
            fw.write("\n");
            fw.write("" + strikes); //Added
            fw.write("\n");
            savedNum = true;//added
            fw.write("" + savedNum);
            fw.write("\n");
            fw.write("" + number); //Added
            fw.write("\n");
            fw.write("" + win);//added
            fw.write("\n");
            fw.write("" + lose);//added
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    //----------------------------------------------------------
    private boolean loadLevel() {
        File file = new File(saveFile);
        if (!file.exists()) {
            return false;
        }
        try (Scanner reader = new Scanner(file)) {
            while (reader.hasNextLine()) {
                int _level = reader.nextInt();
                int _strikes = reader.nextInt(); //added
                boolean _savedNum = reader.nextBoolean();
                int _number = reader.nextInt();//added
                int _win = reader.nextInt();//added
                int _lose = reader.nextInt();//added
                if (_level > 1 & _savedNum == true) { //added
                    level = _level;
                    strikes = _strikes;
                    savedNum = _savedNum;//added
                    number = _number;//added
                    win = _win;//added
                    lose = _lose;//added
                    break;
                }
                else{
                    level = _level;
                    strikes = _strikes;
                    number = _number;
                    win = _win;
                    lose = _lose;
                    break;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e2) {
            e2.printStackTrace();
            return false;
        }
        return level > 1;

    }
    //----------------------------------------------------------
    void run() {
        try (Scanner input = new Scanner(System.in);) {
            System.out.println("Welcome to Number Guesser 4.0!");
            System.out.println("I'll ask you to guess a number between a range, and you'll have " + maxStrikes
                    + " attempts to guess.");
            if (loadLevel()) {
                System.out.println("Successfully loaded level " + level + " and you have " + strikes + " strikes, let's continue then");
                System.out.println("Your current WL ratio is " + (WLratio()));
            }

            if(savedNum == true) {
                System.out.println("A previous guess has been loaded...");
                getRange(level);
            }
            else
            {
                number = getNumber(level);
            }

            isRunning = true;
            while (input.hasNext()) {
                String message = input.nextLine();
                processCommands(message);
                if (!isRunning) {
                    break;
                }
                int guess = getGuess(message);
                processGuess(guess);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }
    //----------------------------------------------------------
    public static void main(String[] args) {
        NumberGuesserHW guesser = new NumberGuesserHW();
        guesser.run();
    }
}