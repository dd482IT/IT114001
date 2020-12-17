import java.util.List; 
import java.util.ArrayList;
import java.util.*;
import java.io.*;


public class muteTransfer{
   
   
   
   
 

   public static void main(String args[]){
   
       List<String> mutedList = new ArrayList<String>();
       mutedList.add("Daniel");
       mutedList.add("Ruben");
       mutedList.add("Alex");
         
       String people = mutedList.toString();
       people = people.replace("[", "");
       people = people.replace("]", "");
       people = people.replace(",","");
       String[] arr = people.split(" "); 
       
       System.out.println(people);
       System.out.println(Arrays.toString(arr));

       
      
         
   }



}