public class RecursionToLoops{

   public static int sum1(int num){
      int x = num; 
      int value = 0;

      for(int i = 0; i < num; i++){
         value+=x;
         x--;
      }
      return value;
   } 
   
   public static int sum(int num) {
		if (num > 0) {
			return num + sum(num - 1);
		}
		return 0;
	}


   public static void main(String args[])
   {
      
      System.out.println(sum(10));
      System.out.println(sum1(10));

   }
}