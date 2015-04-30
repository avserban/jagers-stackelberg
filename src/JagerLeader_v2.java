import comp34120.ex2.PlayerImpl;
import comp34120.ex2.PlayerType;
import comp34120.ex2.Record;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Arrays;

/**
 * The leader for jager team
 * @author L33r and Sh3rb
 */
final class JagerLeader extends PlayerImpl
{
	private Record[] history;
	private double aStar, bStar;
	private final double forgettingFactor = 0.95;
	private final int MAX_WINDOW_SIZE = 101;
	private final int MIN_WINDOW_SIZE = 5;
	private int WINDOW_SIZE = 101;

	private JagerLeader() throws RemoteException, NotBoundException
	{
		super(PlayerType.LEADER, "Jager Leader");
	}

	@Override
	public void startSimulation(final int p_steps) throws RemoteException
	{
		this.history = new Record[MAX_WINDOW_SIZE];

		for (int day = 1; day <= MAX_WINDOW_SIZE - 1; day++)
      this.history[day] = m_platformStub.query(this.m_type, day);


    double[] error = new double[MAX_WINDOW_SIZE];
    for (int day = 2; day <= MAX_WINDOW_SIZE - 1; day++)
    {
      regressionEquation(day);

      for (int i = 1; i <= WINDOW_SIZE; ++i) 
      	error[this.WINDOW_SIZE] += calculateError(this.cache[i]);
    }

    int minimum = 1;
    for (int i = 2; i < BOB; ++i) 
    {
      if (error[i] < error[minimum])
        minimum = i;
    }
    System.out.printf("Size: %2d, Error %.5f\n", minimum, error[minimum]/15);
    this.windowSize = minimum;
	}

	private double calculateError(Record actual) {
    double followerPrice = this.payoff.followerEstimate(actual.m_leaderPrice);
    return Math.abs(followerPrice - actual.m_followerPrice);
  }

	@Override
	public void endSimulation() throws RemoteException
	{
		// print out some statistics
	}

	@Override
	public void proceedNewDay(int p_date) throws RemoteException
	{
		regressionEquation();
   	m_platformStub.publishPrice(m_type, globalMaximum(aStar, bStar));
    updateHistory();
    this.history[100] = m_platformStub.query(this.m_type, p_date);
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
	private double[] regressionEquation(int someDate)
	{
		double sumXSquared = 0;
    	double sumY        = 0;
    	double sumX        = 0;
   	 	double sumXsumY    = 0;

    	int T = someDate - 1;

    	for (int date = someDate - windowSize; date < someDate; ++date) {
      		Record oneDay   = this.cache[date];
      		double lambda = Math.pow(forgettingFactor, T + 1 - date);
      		sumX        += lambda * oneDay.m_leaderPrice;
     		sumY        += lambda * oneDay.m_followerPrice;
     		sumXSquared += lambda * Math.pow(oneDay.m_leaderPrice, 2);
      		sumXsumY    += lambda * oneDay.m_leaderPrice * oneDay.m_followerPrice;
    	}

    	this.aStar = (sumXSquared * sumY - sumX * sumXsumY)  / (T * sumXSquared - Math.pow(sumX, 2));
    	this.bStar = (T * sumXsumY - sumX * sumY) / (T * sumXSquared - Math.pow(sumX, 2));
	}

	private void modifyWindow()
	{

	}

	// This function gives you the follower's estimate value. Use only you have calculated aStar and bStar with the regression Equation
	public double followerEstimate(String equation, double aStar, double bStar, double leaderPrice) {
      if (equation.equals("normal"))
      	return aStar + bStar * leaderPrice;
      else
      	return 
    }

    // This function gives you the global maximum. Use only you have calculated aStar and bStar with the regression Equation
    public float globalMaximum(double aStar, double bStar) {
    	double maxValue = -1, minValue = 1000;
    	for (int day = 1; day <= MAX_WINDOW_SIZE - 1; day++)
    	{
    		if (maxValue < history[day].m_followerPrice)
    			maxValue = history[day].m_followerPrice;

    		if (minValue > history[day].m_followerPrice)
    			minValue = history[day].m_followerPrice;
    	}
    		
    	if bstar >= 0
    		return (float) aStar + bStar * maxValue;
    	else
    		return (float) aStar + bStar * minValue;
    }

    // This function calculates our profit depending on our price and the follower price
    private double profit(double leaderPrice, double followerPrice) {
    	return (leaderPrice - 1.00) * ((2.0 - leaderPrice) + (0.3 * followerPrice));
  	}

  	private double calculateError(Record actual) {
    	double followerPrice = this.followerEstimate(aStar, bStar, actual.m_leaderPrice);
    	return Math.abs(followerPrice - actual.m_followerPrice);
  	}

  	// Rearranges the history when a new day has passed
  	private void updateHistory() {
    	for (int date = 1; date <= 99; ++date) // For each element in the history.
      		history[date] = history[date + 1];
  	}

	public static void main(final String[] p_args) throws RemoteException, NotBoundException
	{
		new JagerLeader();
	}
}


