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
final class JagerLeader_v2 extends PlayerImpl
{
	private Record[] history;
	private double aStar, bStar;
	private final double forgettingFactor = 0.95;
	private final int MAX_WINDOW_SIZE = 101;
	private final int MIN_WINDOW_SIZE = 5;
  private final int timer = 5; //variable to time a restart simulation
	private int WINDOW_SIZE = 101;

	private JagerLeader_v2() throws RemoteException, NotBoundException
	{
		super(PlayerType.LEADER, "Jager Leader");
	}


  /**
  * This method computes the ideal window size used in the actual simulation.
  **/
	@Override
	public void startSimulation(final int p_steps) throws RemoteException
	{
    // populate history array with prices from the platform
		this.history = new Record[MAX_WINDOW_SIZE];
		for (int day = 1; day < MAX_WINDOW_SIZE; day++)
      this.history[day] = m_platformStub.query(this.m_type, day);  

    double[] error = new double[MAX_WINDOW_SIZE];
    for (this.WINDOW_SIZE = 1; this.WINDOW_SIZE < MAX_WINDOW_SIZE - 1; ++this.WINDOW_SIZE)
    {
      regressionEquation(MAX_WINDOW_SIZE - 1); //TODO: make sure this is accurate
      for (int i = 1; i < MAX_WINDOW_SIZE; ++i) 
      	error[WINDOW_SIZE] += calculateError(this.history[i]);
    }

    int minimum = 1;
    for (int i = 1; i < MAX_WINDOW_SIZE - 1; ++i) 
      if (error[i] < error[minimum])
        minimum = i;

    System.out.printf("Size: %2d, Error %.5f\n", minimum, error[minimum]/100);
    this.WINDOW_SIZE = MAX_WINDOW_SIZE - minimum - 1;
	}

	private double calculateError(Record actual) {
    double followerPrice = followerEstimate("normal", aStar, bStar, actual.m_leaderPrice);
    return Math.pow(followerPrice - actual.m_followerPrice, 2);
  }

	@Override
	public void proceedNewDay(int p_date) throws RemoteException
	{
    //check if we need to recompute window size
    if (p_date % timer == 0)
      updateWindowSize(); 

		regressionEquation(p_date);
   	m_platformStub.publishPrice(m_type, globalMaximum(aStar, bStar));
    updateHistory();
    this.history[100] = m_platformStub.query(this.m_type, p_date);
	}


  public void updateWindowSize()
  {
    // TODO: must check the current windowsize and find the smallest error and reupdate windowsize
    double[] error = new double[WINDOW_SIZE];
    int lastWindowSize = WINDOW_SIZE;
    for (this.WINDOW_SIZE = 1; this.WINDOW_SIZE < lastWindowSize; ++this.WINDOW_SIZE)
    {
      regressionEquation(lastWindowSize); //TODO: make sure this is accurate
      for (int i = 1; i < lastWindowSize; ++i) 
        error[WINDOW_SIZE] += calculateError(this.history[i]);
    }

    int minimum = 1;
    for (int i = 1; i < lastWindowSize; ++i) 
    {
      if (error[i] < error[minimum])
        minimum = i;
    }
    System.out.printf("Size: %2d, Error %.5f\n", minimum, error[minimum]/lastWindowSize);
    this.WINDOW_SIZE = lastWindowSize - minimum - 1;
  }


	// This function calculates the regression equation for the past 100 days needed to approximate response function for the follower
	private void regressionEquation()
	{
		double sumXSquared = 0;
    double sumY        = 0;
    double sumX        = 0;
   	double sumXsumY    = 0;
   	int T = 100;

   	for (int date = 1; date <= T; date++)
   	{
   	  Record oneDay = this.history[date];
   	 	double lambda = Math.pow(forgettingFactor, T + 1 - date);
   	 	sumX        += oneDay.m_leaderPrice;
      sumY        += oneDay.m_followerPrice;
      sumXSquared += Math.pow(oneDay.m_leaderPrice, 2);
      sumXsumY    += oneDay.m_leaderPrice * oneDay.m_followerPrice;
   	}

   	this.aStar = (sumXSquared * sumY - sumX * sumXsumY)  / (T * sumXSquared - Math.pow(sumX, 2));
   	this.bStar = (T * sumXsumY - sumX * sumY) / (T * sumXSquared - Math.pow(sumX, 2));
	}

	// This function calculates the regression equation for a variable number of days before the parameter
	private void regressionEquation(int someDate)
	{
		double sumXSquared = 0;
    double sumY        = 0;
    double sumX        = 0;
   	double sumXsumY    = 0;
    Record oneDay;

    int T = someDate - 1;
    for (int date = someDate - WINDOW_SIZE; date < someDate; ++date) {
      System.out.println(someDate + "   " + WINDOW_SIZE + "    " + date);
      if (date > 100)
        oneDay = this.history[date - (date % 100)];
      else
        oneDay = this.history[date];
      double lambda = Math.pow(forgettingFactor, T + 1 - date);
      sumX        += lambda * oneDay.m_leaderPrice;
     	sumY        += lambda * oneDay.m_followerPrice;
     	sumXSquared += lambda * Math.pow(oneDay.m_leaderPrice, 2);
      sumXsumY    += lambda * oneDay.m_leaderPrice * oneDay.m_followerPrice;
    }

    this.aStar = (sumXSquared * sumY - sumX * sumXsumY)  / (T * sumXSquared - Math.pow(sumX, 2));
    this.bStar = (T * sumXsumY - sumX * sumY) / (T * sumXSquared - Math.pow(sumX, 2));
	}


	  // This function gives you the follower's estimate value. Use only you have calculated aStar and bStar with the regression Equation
	  public double followerEstimate(String equation, double aStar, double bStar, double leaderPrice) {
      	return aStar + bStar * leaderPrice;
    }

    // TODO: Change so that the it can calculate other types of equations based on a flag.
    //
    // This function gives you the global maximum. Use only you have calculated aStar and bStar with the regression Equation
    public float globalMaximum(double aStar, double bStar) {
      return (float) ((2.7 + 0.3 * aStar) / (2.0 - 0.6 * bStar));
    	// double maxValue = -1, minValue = 1000;
    	// for (int day = 1; day <= MAX_WINDOW_SIZE - 1; day++)
    	// {
    	// 	if (maxValue < history[day].m_followerPrice)
    	// 		maxValue = history[day].m_followerPrice;

    	// 	if (minValue > history[day].m_followerPrice)
    	// 		minValue = history[day].m_followerPrice;
    	// }
    		
    	// if (bStar >= 0)
    	// 	return (float) aStar + bStar * maxValue;
    	// else
    	// 	return (float) aStar + bStar * minValue;
    }

    // This function calculates our profit depending on our price and the follower price
    private double profit(double leaderPrice, double followerPrice) {
    	return (leaderPrice - 1.00) * ((2.0 - leaderPrice) + (0.3 * followerPrice));
  	}

  	// Rearranges the history when a new day has passed
  	private void updateHistory() {
    	if (WINDOW_SIZE < MAX_WINDOW_SIZE)
        WINDOW_SIZE++;

      for (int date = 1; date <= 99; ++date) // For each element in the history.
      		history[date] = history[date + 1];
  	}

	public static void main(final String[] p_args) throws RemoteException, NotBoundException
	{
		new JagerLeader_v2();
	}
}


