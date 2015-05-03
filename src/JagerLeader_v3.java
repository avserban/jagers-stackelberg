import comp34120.ex2.PlayerImpl;
import comp34120.ex2.PlayerType;
import comp34120.ex2.Record;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Arrays;

//TODO: Implement method that goes through the history and reconstructs a function using 
// the followers values.

//TODO: look into http://introcs.cs.princeton.edu/java/97data/PolynomialRegression.java.html
//      and also install JAMA (java matrix bs)

/**
 * The leader for jager team
 * @author L33r and Sh3rb
 */
final class JagerLeader_v3 extends PlayerImpl
{
	private Record[] history;
	private double aStar, bStar;
	private final double forgettingFactor = 0.95;
	private final int MAX_WINDOW_SIZE = 131;
	private final int MIN_WINDOW_SIZE = 5;
  private int START_WINDOWS_INDEX;
  private int END_WINDOWS_INDEX;
	private int WINDOW_SIZE = 101;
  private double[] historyError;

	private JagerLeader_v3() throws RemoteException, NotBoundException
	{
		super(PlayerType.LEADER, "Jager Leader");
	}

  //This method computes the ideal window size used in the actual simulation.
	@Override
	public void startSimulation(final int p_steps) throws RemoteException
	{
    START_WINDOWS_INDEX = 1;
    END_WINDOWS_INDEX = 2;

    // populate history array with prices from the platform
		this.history = new Record[MAX_WINDOW_SIZE];
		for (int day = 1; day <= 100; day++)
      this.history[day] = m_platformStub.query(this.m_type, day);  

    historyError = new double[MAX_WINDOW_SIZE];

    while(END_WINDOWS_INDEX <= 100)
    {
      historyError[END_WINDOWS_INDEX] = rSquare(START_WINDOWS_INDEX, END_WINDOWS_INDEX);
      if (Math.abs(historyError[END_WINDOWS_INDEX] - historyError[END_WINDOWS_INDEX - 1]) < 0.00001)
        START_WINDOWS_INDEX = END_WINDOWS_INDEX;
      System.out.println("Day " + END_WINDOWS_INDEX + ": " + START_WINDOWS_INDEX + " " + END_WINDOWS_INDEX + " " + historyError[END_WINDOWS_INDEX] + " Difference: " + (historyError[END_WINDOWS_INDEX] - historyError[END_WINDOWS_INDEX - 1]));
      END_WINDOWS_INDEX ++;
    }
    WINDOW_SIZE = END_WINDOWS_INDEX - START_WINDOWS_INDEX + 1;
	}

	private double calculateError(Record actual) {
    double followerPrice = followerEstimate("normal", aStar, bStar, actual.m_leaderPrice);
    return Math.pow(followerPrice - actual.m_followerPrice, 2);
  }

	@Override
	public void proceedNewDay(int p_date) throws RemoteException
	{
    //check if we need to recompute window size
    //System.out.println("A*: " + aStar + "----------------------- B*: " + bStar);
    System.out.println("Current day: " + p_date);
   	m_platformStub.publishPrice(m_type, globalMaximum(aStar, bStar));
    this.history[p_date] = m_platformStub.query(this.m_type, p_date);

    //regressionEquation(START_WINDOWS_INDEX, END_WINDOWS_INDEX - 1);
    System.out.println(START_WINDOWS_INDEX + " " + END_WINDOWS_INDEX);
    
    historyError[END_WINDOWS_INDEX] = rSquare(START_WINDOWS_INDEX, END_WINDOWS_INDEX);
    if (Math.abs(historyError[END_WINDOWS_INDEX] - historyError[END_WINDOWS_INDEX - 1]) < 0.01)
      START_WINDOWS_INDEX = END_WINDOWS_INDEX;
    // System.out.println("Day " + END_WINDOWS_INDEX + ": " + START_WINDOWS_INDEX + " " + END_WINDOWS_INDEX + " " + historyError[END_WINDOWS_INDEX] + " Difference: " + (historyError[END_WINDOWS_INDEX] - historyError[END_WINDOWS_INDEX - 1]));  
    END_WINDOWS_INDEX ++;
	}

  // This function calculates the regression equation for a variable number of days before the parameter
  private void regressionEquation(int startDate, int endDate)
  {
    // Have some variables for the terms found in the a* and b* equations
    double sumOfXSquared = 0;
    double sumOfY = 0;
    double sumOfX = 0;
    double sumOfXsumOfY = 0;
    Record oneDay;
    int T = endDate;

    // Calculating all the sums
    for (int date = startDate; date <= endDate; ++date) {
      oneDay = this.history[date];

      double lambda = Math.pow(forgettingFactor, T + 1 - date);
      sumOfX += lambda * oneDay.m_leaderPrice;
      sumOfY += lambda * oneDay.m_followerPrice;
      sumOfXSquared += lambda * Math.pow(oneDay.m_leaderPrice, 2);
      sumOfXsumOfY += lambda * oneDay.m_leaderPrice * oneDay.m_followerPrice;
    }

    // calculate a* and b*
    this.aStar = (sumOfXSquared * sumOfY - sumOfX * sumOfXsumOfY)  / (T * sumOfXSquared - Math.pow(sumOfX, 2));
    this.bStar = (T * sumOfXsumOfY - sumOfX * sumOfY) / (T * sumOfXSquared - Math.pow(sumOfX, 2));
    System.out.println("A*: " + aStar + "----------------------- B*: " + bStar);
  }

  private double rSquare(int startDate, int endDate)
  {
    double yBar = 0, sse = 0, sst = 0;
    double sumOfFollowerPrice = 0, sumOfFollowerEstimation = 0;
    double followerProfit = 0;

    regressionEquation(startDate, endDate);

    //Calculate sums of follower prices and estimations
    for (int i = startDate; i <= endDate; i ++)
    {
      sumOfFollowerPrice += history[i].m_followerPrice;
      sumOfFollowerEstimation += followerEstimate("normal", aStar, bStar, history[i].m_leaderPrice);
    }

    //System.out.println("Follower estimation: " + sumOfFollowerEstimation);
    //System.out.println("Follower price: " + sumOfFollowerPrice);

    //Calculate Y Bar
    yBar = (1 / (endDate - startDate + 1)) * sumOfFollowerPrice;
    //System.out.println("Y Bar: " + yBar);

    //Calculate sums of follower prices and estimations
    for (int i = startDate; i <= endDate; i ++)
    {
      sst += Math.pow(history[i].m_followerPrice - yBar, 2);
      sse += Math.pow(history[i].m_followerPrice - followerEstimate("normal", aStar, bStar, history[i].m_leaderPrice), 2);
    }
    //System.out.println("SSE: "+ sse);
    //System.out.println("SST: "+ sst);
    //System.out.println("R Square: " + Math.sqrt(1 - (sse / sst)));
    return (1 - (sse / sst)); 
  }

	// This function calculates the regression equation for a variable number of days before the parameter
	private void regressionEquation(int someDate)
	{
    // Have some variables for the terms found in the a* and b* equations
		double sumOfXSquared = 0;
    double sumOfY = 0;
    double sumOfX = 0;
   	double sumOfXsumOfY = 0;
    Record oneDay;
    int T = someDate - 1;

    // Calculating all the sums
    for (int date = someDate - WINDOW_SIZE; date < someDate; ++date) {
      oneDay = this.history[date];

      double lambda = Math.pow(forgettingFactor, T + 1 - date);
      sumOfX += lambda * oneDay.m_leaderPrice;
      sumOfY += lambda * oneDay.m_followerPrice;
      sumOfXSquared += lambda * Math.pow(oneDay.m_leaderPrice, 2);
      sumOfXsumOfY += lambda * oneDay.m_leaderPrice * oneDay.m_followerPrice;
    }

    // calculate a* and b*
    this.aStar = (sumOfXSquared * sumOfY - sumOfX * sumOfXsumOfY)  / (T * sumOfXSquared - Math.pow(sumOfX, 2));
    this.bStar = (T * sumOfXsumOfY - sumOfX * sumOfY) / (T * sumOfXSquared - Math.pow(sumOfX, 2));
	}


	// This function gives you the follower's estimate value. Use only you have calculated aStar and bStar with the regression Equation
	public double followerEstimate(String equation, double aStar, double bStar, double leaderPrice) {
    return aStar + bStar * leaderPrice;
  }

  // This function gives you the global maximum. Use only you have calculated aStar and bStar with the regression Equation
  public float globalMaximum(double aStar, double bStar) {
    return (float) ((3 + 0.3 * aStar - 0.3 * bStar) / (2 - 0.6 * bStar));
  }

	public static void main(final String[] p_args) throws RemoteException, NotBoundException {
		new JagerLeader_v3();
	}
}