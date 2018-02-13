package ltCMP;

import java.util.List;

import general.CMPDataCenter;
import general.Customer;
import general.DataCenter;

public interface CMPFiller {

	public void populate(CMPDataCenter dc, List<Customer> app, float tolerance);
}
