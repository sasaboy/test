import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.xerces.util.XMLChar;

public class evaluationorder {
   public static void main(String[] args) throws Throwable {
      Map m = new LinkedHashMap();
       m.put("var1", "\"It uses nothing at all\"");
       m.put("var2", "\"It uses \"+var7");
       m.put("var3", "\"It uses \"+var1 +\" and \"+var4");
       m.put("var4", "\"It uses nothing again\"");
       m.put("var5", "\"It uses \"+var6");
       m.put("var6", "\"It uses \" + var2");
       m.put("var7", "\"It uses \"  +  var6");

//       m.put("varb", "\"It uses \"+ vara +\" if possible \"");
//       m.put("vare", "\"It uses \"+ varb +\" if possible varc and vard\"");
//       m.put("vara", "\"It uses var\"");
//       m.put("varc", "\"It uses \"+varb+\" if possible \"");
//       m.put("vard", "\"It uses \"+varc +\" if possible and varb\"");

//      m.put("varb", "\"I am varb and I use \"+ vara");
//      m.put("vard", "\"I'm vard and I use vara and varc: \"+ vara +\" and \"+varc");
//      m.put("vara", "\"I'm vara and I don't use anything");
//      m.put("varc", "varb + \" is used by me, varc\"");

      System.out.println("Sorted=" + determineVariableEvaluationOrder(m));
//      System.out.println("Sorted=" + determineVariableEvaluationOrderOld(ret, m));

   }

   static List<String> determineVariableEvaluationOrder(Map<String, String> variables)
      throws Exception {
      List<String> sortList = new ArrayList<String>(variables.keySet());
      List<String> ret = new ArrayList<String>(sortList);
      System.out.println("SL=" + sortList);
      int[][] matrix = new int[ret.size()][ret.size()];
      for (int i = 0; i < sortList.size(); i++) {
         String id2handle = sortList.get(i);
         for (int j = 0; j < ret.size(); j++) {
            String id = ret.get(j);
            String value = variables.get(id);
            int ups = getUsingPositions(value, id2handle, variables, true, true).size();
            matrix[i][j] = ups;
         }
      }
      for (int i = 0; i < matrix.length; i++) {
         System.out.println();
         for (int j = 0; j < matrix.length; j++) {
            System.out.print(matrix[i][j] + " ");
         }
      }

      for (int i = 0; i < matrix.length; i++) {
         if (matrix[i][i] > 0) {
            throw new Exception("Self references are NOT allowed - variable "
                                + sortList.get(i) + " is referencing itself!");
         }
      }
      for (int i = 0; i < matrix.length; i++) {
         for (int j = 0; j < matrix.length; j++) {
            if (matrix[i][j] > 0 && matrix[j][i] > 0) {
               throw new Exception("Cross-references are NOT allowed - variables "
                                   + sortList.get(i) + " and " + sortList.get(j)
                                   + " are referencing each other!");
            }
         }
      }
      for (int i = 0; i < matrix.length; i++) {
         int[] rows = getRowsOrColumns(matrix, i, true);
         boolean matches = match(matrix, rows, i, new ArrayList());
         if (matches) {
            throw new Exception("Implicit cross-references are NOT allowed - variable "
                                + sortList.get(i) + " has implicit cross-reference!");
            // System.exit(1);
         }
      }
      System.out.println();
      // move the entries that do not reference other entries to the beginning of the list
      for (int i = 0; i < matrix.length; i++) {
         int[] rows = getRowsOrColumns(matrix, i, true);
         if (rows.length == 0) {
            String toReplace = sortList.get(i);
            ret.remove(toReplace);
            ret.add(0, toReplace);
         }
      }
      // move the entries which are not referenced by other entries to the end of the list
      for (int i = 0; i < matrix.length; i++) {
         int[] cols = getRowsOrColumns(matrix, i, false);
         if (cols.length == 0) {
            String toReplace = sortList.get(i);
            ret.remove(toReplace);
            ret.add(toReplace);
         }
      }

      // now go through the list and reposition entries again (sequentially iterating
      // through entries)
      sortList = new ArrayList(ret);

      System.out.println("SL=" + sortList);
      for (int i = 0; i < sortList.size(); i++) {
         String id2handle = sortList.get(i);
         Iterator<Map.Entry<String, String>> it = variables.entrySet().iterator();
         while (it.hasNext()) {
            Map.Entry<String, String> me = it.next();
            String id = (String) me.getKey();
            if (id.equals(id2handle))
               continue;
            String value = (String) me.getValue();

            // if the variable 'id' contains variable 'id2handle' insure that position of
            // the variable 'id2handle' is before variable 'id'
            int ups = getUsingPositions(value, id2handle, variables, true, true).size();
            System.out.println("Searching for "
                               + id2handle + " within " + id + "'s content:" + value
                               + ", found " + ups);
            if (value != null && ups > 0) {
               int ind1 = ret.indexOf(id2handle);
               int ind2 = ret.indexOf(id);
               System.out.println("Moving "
                                  + id2handle + " before " + id
                                  + ", indid2handle=" + ind1 + ", indid=" + ind2);
               if (ind1 > ind2) {
                  ret.remove(ind1);
                  ret.add(ind2, id2handle);
               }
            }
         }
      }
      return ret;
   }

   private static boolean match(int[][] matrix, int[] is, int j, List processed) {
      System.out.println();
      System.out.print("MATCH " + j + "->");
      printintarr(is);
      if (contains(is, j)) {
         return true;
      }
      for (int k = 0; k < is.length; k++) {
         if (!processed.contains(is[k])) {
            processed.add(is[k]);
            int[] rows = getRowsOrColumns(matrix, is[k], true);
            boolean ret = match(matrix, rows, j, processed);
            return ret;
         }
      }

      return false;
   }

   private static boolean contains(int[] is, int j) {
      for (int k = 0; k < is.length; k++) {
         if (is[k] == j) {
            return true;
         }
      }
      return false;
   }

   private static int[] getRowsOrColumns(int[][] matrix, int rOrC, boolean searchRows) {
      List<Integer> ret = new ArrayList<Integer>();
      for (int rOrCCnt = 0; rOrCCnt < matrix.length; rOrCCnt++) {
         if (searchRows) {
            if (matrix[rOrCCnt][rOrC] > 0) {
               ret.add(new Integer(rOrCCnt));
            }
         } else {
            if (matrix[rOrC][rOrCCnt] > 0) {
               ret.add(new Integer(rOrCCnt));
            }
         }
      }
      int[] rOrCs = new int[ret.size()];
      for (int k = 0; k < ret.size(); k++) {
         rOrCs[k] = ret.get(k).intValue();
      }
      return rOrCs;
   }

   static void printintarr(int[] arr) {
      for (int i = 0; i < arr.length; i++) {
         System.out.print(arr[i]);
         if (i < arr.length - 1)
            System.out.print(",");
      }
   }

   // private static int[] getis(int[][] matrix, int j) {
   // List<Integer> ret = new ArrayList<Integer>();
   // for (int i = 0; i < matrix.length; i++) {
   // if (matrix[i][j] > 0) {
   // ret.add(new Integer(i));
   // }
   // }
   // int[] is = new int[ret.size()];
   // for (int k = 0; k < ret.size(); k++) {
   // is[k] = ret.get(k).intValue();
   // }
   // return is;
   // }
   //
   // private static int[] getjs(int[][] matrix, int i) {
   // List<Integer> ret = new ArrayList<Integer>();
   // for (int j = 0; j < matrix.length; j++) {
   // if (matrix[i][j] > 0) {
   // ret.add(new Integer(j));
   // }
   // }
   // int[] js = new int[ret.size()];
   // for (int k = 0; k < ret.size(); k++) {
   // js[k] = ret.get(k).intValue();
   // }
   // return js;
   // }

   /**
    * Returns the list of positions (Integers) of string 'dfOrFpId' within the string
    * 'expr'.
    * 
    * @param expr String to check.
    * @param dfOrFpId Id of {@link DataField} or {@link FormalParameter}.
    * @param allVars Map of variables that can occur within expression.
    * @param checkPrevAndNextCharacter true if characters prior and post to dfOrFpId
    *           occurence should be checked for validity
    * @return The list of positions (Integers) of string 'dfOrFpId' within the string
    *         'expr'.
    */
   public static List getUsingPositions(String expr,
                                        String dfOrFpId,
                                        Map allVars,
                                        boolean checkPrevAndNextCharacter,
                                        boolean checkText) {
      System.out.println("GUP of " + dfOrFpId + " in " + expr);
      List positions = new ArrayList();
      if (expr.trim().equals("") || dfOrFpId.trim().equals(""))
         return positions;
      String exprToParse = new String(expr);
      int foundAt = -1;
      while ((foundAt = exprToParse.indexOf(dfOrFpId)) >= 0) {
         // System.out.println("Searching for using positions of variable "+dfOrFpId+" in expression "+exprToParse+" -> found "+foundAt);
         if (foundAt < 0)
            break;
         if (exprToParse.equals(dfOrFpId)) {
            int pos = foundAt;
            if (positions.size() > 0) {
               pos += ((Integer) positions.get(positions.size() - 1)).intValue()
                      + dfOrFpId.length();
            }
            positions.add(new Integer(pos));
            break;
         }
         boolean prevOK = false, nextOK = false;
         char prev, next;
         // if given Id is found within expression string
         // check if Id string is part of some other Id name

         if (checkPrevAndNextCharacter) {
            // check if char previous to the position of found Id is OK
            if (foundAt == 0) {
               prevOK = true;
            } else {
               prev = exprToParse.charAt(foundAt - 1);
               prevOK = !isIdValid(String.valueOf(prev)) || prev == ':';
               // System.out.println("Is prev char "+prev+" ok = "+prevOK);
            }

            // check if char after found ID string is OK
            if (foundAt + dfOrFpId.length() == exprToParse.length()) {
               nextOK = true;
            } else {
               next = exprToParse.charAt(foundAt + dfOrFpId.length());
               nextOK = !isIdValid(String.valueOf(next));
               if (!nextOK && (next == '-' || next == '.')) {
                  nextOK = true;
                  List varIdsWithChar = new ArrayList(allVars.keySet());
                  Iterator li = varIdsWithChar.iterator();
                  while (li.hasNext()) {
                     String vid = (String) li.next();
                     if (vid.indexOf(next) <= 0) {
                        li.remove();
                     }
                  }
                  if (varIdsWithChar.size() > 0) {
                     li = varIdsWithChar.iterator();
                     while (li.hasNext()) {
                        String vid = (String) li.next();
                        int ovp = exprToParse.indexOf(vid);
                        if (ovp == foundAt) {
                           nextOK = false;
                           break;
                        }
                     }
                  }
               }
               // System.out.println("Is next char "+next+" ok = "+nextOK);
            }
         }
         boolean isTxt = true;
         int indofplus = exprToParse.lastIndexOf("+", foundAt);
         int indofquotes = exprToParse.lastIndexOf("\"", indofplus);
         if (foundAt == 0
             || (foundAt > indofplus && indofplus > indofquotes && indofquotes >= 0)) {
            if (foundAt > 0) {
               if (exprToParse.substring(indofquotes + 1, indofplus).trim().equals("")
                   && exprToParse.substring(indofplus + 1, foundAt).trim().equals("")) {
                  isTxt = false;
               }
            } else {
               isTxt = false;
            }
         }
         System.out.println("FA="
                            + foundAt + ", indofplus=" + indofplus + ", indofq="
                            + indofquotes + ", isTxt=" + isTxt);

         // if this is really the Id, add its position in expression
         if ((!checkPrevAndNextCharacter || (prevOK && nextOK)) && (!checkText || !isTxt)) {
            int pos = foundAt;
            if (positions.size() > 0) {
               pos += ((Integer) positions.get(positions.size() - 1)).intValue()
                      + dfOrFpId.length();
            }
            positions.add(new Integer(pos));
         }
         exprToParse = exprToParse.substring(foundAt + dfOrFpId.length());
         // System.out.println("New expr to parse is "+exprToParse);
      }
      // System.out.println("Using positions of variable "+dfOrFpId+" for expression: "+expr+", "+positions);
      return positions;
   }

   public static boolean isIdValid(String id) {
      return id != null && XMLChar.isValidNmtoken(id) && !id.trim().equals("");
   }

}
