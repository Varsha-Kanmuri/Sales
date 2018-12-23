import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BookSales {
    public static void main(String[] args) {

        /*The program accepts the following commnad line arguments below :
        java BookSales C:///books_list.csv C:///sales_list.csv --top_selling_books=4 --top_customers=3 --sales_on_date=2017-12-22 */
        String booksFileName = args[0];
        String salesFileName = args[1];

        String cmdSeparator = "="; //separator to extract values given to optional arguments in command prompt
        String SEPARATOR = ","; //csv file separator

        //calling functions to parse book_list.csv and sales_list.csv files
        BookSales sale = new BookSales();
        List<String> booksData = sale.parseCsvFile(booksFileName);
        List<String> saleData = sale.parseCsvFile(salesFileName);

        //Storing the Book Id and corresponding Price of each book obtained from parsed book_list.csv file
        Map<String,Double> bookCostMap = sale.getBookPrice(booksData, SEPARATOR);

        //Storing each sale record corresponding to parsed sales_list.csv file into a list of sales type objects
        List<Sales> salesRecord = sale.getSalesRecords(saleData, SEPARATOR);

        //Below three fields are optional fields given in the command arguments
        //executes only if the count of top selling books to be found given in the input command arguments
        String[] topSellingBook = args[2].split(cmdSeparator);
        if(topSellingBook.length > 1) {
            int topBooksCount = Integer.parseInt(topSellingBook[1]);
            List<String> topSellingBooks = sale.getTopSellingBooks(salesRecord, bookCostMap);
            System.out.print("top_selling_books\t");
            try {
                IntStream.range(0, topBooksCount).mapToObj(i -> topSellingBooks.get(i) + "\t").forEach(System.out::print);
            } catch(IndexOutOfBoundsException exception) { //throws error if we try to give a value beyond the number of book IDS being sold (in csv this range is 4)
                System.out.println("\nNot enough books to print, max range is till\t" +topSellingBooks.size());
            }
            System.out.println();
        }

        //executes only if the count of top customers to be found is given in the input command arguments
        String[] topCustomer = args[3].split(cmdSeparator);
        if(topCustomer.length > 1) {
            int topCustomerCount = Integer.parseInt(topCustomer[1]);
            List<String> topCustomers = sale.getTopCustomer(salesRecord, bookCostMap);
            System.out.print("top_customers\t");
            try {
                IntStream.range(0, topCustomerCount).mapToObj(i -> topCustomers.get(i) + "\t").forEach(System.out::print);
            } catch(IndexOutOfBoundsException exception) { //throws error if we try to give a value beyond number of existing customer emails (in csv this range is 3)
                System.out.println("\nNot enough customers to print, max range is till\t" +topCustomers.size());
            }
            System.out.println();
        }

        //executes only if the date field given in input command arguments
        String[] dateGiven = args[4].split(cmdSeparator);
        if(dateGiven.length > 1) {
            String date = dateGiven[1];
            List<Sales> salesOnDate = sale.getSalesDetailsOnDate(date, salesRecord);
            double totalSaleOnDate = sale.getTotalSales(salesOnDate, bookCostMap);
            System.out.println("sales_on_date\t"+ date + "\t" + totalSaleOnDate);
        }
    }

    //This is a common function to parse both the csv files into a list of strings corresponding to each record/row in the csv file
    private List<String> parseCsvFile(String fileName) {
        BufferedReader br = null;
        List<String> list = new ArrayList<>();
        try
        {
            //Reading the csv file
            br = Files.newBufferedReader(Paths.get(fileName));
            //Read to skip the header
            br.readLine();
            list = br.lines().collect(Collectors.toList());
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    //Taking into account only book ID and book price from the csv list , since only these two fields required to solve the problem
    //The map contaings key as the Book ID and price of book as the value
    private Map<String,Double> getBookPrice(List<String> booksData, String separator) {
        Map<String,Double> bookPrice = new HashMap<String,Double>();

        for(String bookRow : booksData) {
            String[] bookDetail = bookRow.split(separator);
            bookPrice.put(bookDetail[0],priceConverter(bookDetail[3]));
        }
        return bookPrice;
    }

    //This function converts the price of book to respective format, I assume it to be of type double
    private double priceConverter(String bookPrice) {
        try {
            return Double.parseDouble(bookPrice);
        } catch (NumberFormatException e) {
            System.out.println("Conversion to the desired format failed with the message " + e.getMessage());
            return 0.0;//can return any default value
        }
    }

    //This function stores each sale record corresponding to parsed sales_list.csv file into a list of Sales class(a separate class defined below) type objects
    private List<Sales> getSalesRecords(List<String> salesData, String separator) {

        List<Sales> salesList = new ArrayList<Sales>();
        for(String salesRow : salesData) {
            String[] saleDetail = salesRow.split(separator);
            Sales sale = new Sales();
            sale.setDate(saleDetail[0]);
            sale.setClientEmail(saleDetail[1]);
            sale.setPaymentMethod(saleDetail[2]);
            sale.setItemCount(Integer.parseInt(saleDetail[3]));
            int startItemIndex= 4; //starts from 4th index
            int itemCounter = sale.getItemCount();
            Map<String,Integer> itemsPurchased = new HashMap<String,Integer>();
            while(itemCounter > 0) {
                String[] items = saleDetail[startItemIndex].split(";");
                int quantity = Integer.parseInt(items[1]);
                itemsPurchased.put(items[0],quantity);
                itemCounter -= quantity;
                startItemIndex++;
            }
            sale.setItemsPurchased(itemsPurchased);
            salesList.add(sale);
        }
        return salesList;
    }

    //This function returns the List of Sale records on the specified date passed
    private List<Sales> getSalesDetailsOnDate(String date, List<Sales> salesList){

        return salesList.stream()
                .filter(t -> t.getDate().equals(date))
         .collect(Collectors.toList());
    }

    //This function calculates and returns the totalamount returned by multiplying price of the books obtained from bookCostMap with quantity of books from Sales object
    //ie.. uses basic formula of total = (price*quantity) to find the purchase made by each sale record
    private double getTotalSales(List<Sales> salesList, Map<String,Double> bookCostMap)  {

        double totalAmount = 0;
        for(Sales sale : salesList) {
            Map<String,Integer> items = sale.getItemsPurchased();
            for (Map.Entry<String, Integer> m : items.entrySet())
                if(bookCostMap.containsKey(m.getKey())) {
                    String bookId = m.getKey();
                    totalAmount += m.getValue() * bookCostMap.get(bookId);
                }
        }
        return totalAmount;
    }


    //This function returns the list of all  customer email IDs arranged in descending order of value of their purchases/profit made
    private List<String> getTopCustomer(List<Sales> salesOnDate, Map<String,Double> bookCostMap) {

        List<String> topCustomers = new ArrayList<>();
        Map<String, Double> customerProfitList = new HashMap<>();
        Map<String, List<Sales>> salesPerCustomer = salesOnDate.stream()
                .collect(Collectors.groupingBy(Sales::getClientEmail));

        for (Map.Entry<String, List<Sales>> m : salesPerCustomer.entrySet()) {
            String customerMail = m.getKey();
            double customerProfit = getTotalSales(m.getValue(), bookCostMap);
            customerProfitList.put(customerMail, customerProfit);
            }
         customerProfitList.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .forEachOrdered(x -> topCustomers.add(x.getKey()));

        return topCustomers;
    }

    //This function returns the list of all Book IDs arranged in descending order of value of their purchases/profit made
    private List<String> getTopSellingBooks(List<Sales> salesList, Map<String,Double> bookCostMap){
        List<String> topBooks = new ArrayList<>();
        Map<String, Double> bookProfitList = new HashMap<>();

        for(Sales sale : salesList) {
            Map<String,Integer> items = sale.getItemsPurchased();
            for (Map.Entry<String, Integer> m : items.entrySet())
                if(bookCostMap.containsKey(m.getKey())) {
                    String bookId = m.getKey();
                    if(bookProfitList.containsKey(bookId)){
                        double amount = bookProfitList.get(bookId);
                        double totalAmount = amount + (m.getValue() * bookCostMap.get(bookId));
                        bookProfitList.put(bookId, totalAmount);
                    } else {
                        bookProfitList.put(bookId, m.getValue() * bookCostMap.get(bookId));
                    }
                }
        }

        bookProfitList.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .forEachOrdered(x -> topBooks.add(x.getKey()));
        return topBooks;
    }

}


class Sales {
    //keeping date in string format itself as it is only needed to filter results in problem
    private String saleDate;
    private String clientEmail;
    private String paymentMethod;
    private int itemCount;
    private Map<String,Integer> itemsPurchased;

    public void setDate(String saleDate) {
        this.saleDate = saleDate;
    }

    public String getDate() {
        return saleDate;
    }

    public void setClientEmail(String clientEmail) {
        this.clientEmail = clientEmail;
    }

    public String getClientEmail() {
        return clientEmail;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public void setItemCount(int itemCount) {
        this.itemCount =  itemCount;
    }

    public int getItemCount() {
        return itemCount;
    }

    public void setItemsPurchased(Map<String,Integer> itemPurchased) {
        this.itemsPurchased = itemPurchased;
    }

    public Map<String,Integer> getItemsPurchased() {
        return itemsPurchased;
    }

}
