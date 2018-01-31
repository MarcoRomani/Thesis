package cpp_heuristics;

import java.util.ArrayList;
import java.util.List;

import general.Container;
import general.Customer;
import general.DataCenter;
/**
 * 
 * @author Marco
 * This neighborhood is similar to CPPOneSwapIter, but is smaller, meaning that only intra-customer swap are performed.
 * Therefore, fewer combinations exist.
 */
public class CPPOneSwapSmallIter extends CPPOneSwapIter {

	protected List<Customer> custs = new ArrayList<Customer>();
	protected int cust_index;
	protected List<Container> allconts = new ArrayList<Container>();

	public CPPOneSwapSmallIter() {
		for (Customer c : Customer.custList) {
			if (c.getNewContainers().size() > 1) {
				custs.add(c);
			}

		}
	}

	@Override
	public boolean hasNext() {
		if (cust_index + index_one + index_two >= custs.size() + 2 * conts.size() - 4) {
			stubs.get(sol.getTable().get(conts.get(index_one)).intValue()).allocate(conts.get(index_one), stubs, copy,
					dc, true);
			copy.getTable().put(conts.get(index_one), sol.getTable().get(conts.get(index_one)));
			return false;
		}
		return true;
	}

	@Override
	public void setUp(DataCenter dc, List<ServerStub> stubs, CPPSolution sol) {

		this.dc = dc;
		this.stubs = stubs;

		index_one = 0;
		index_two = 0;

		List<Container> toSwap = new ArrayList<Container>();
		for (Container vm : allconts) {
			if (this.sol.getTable().get(vm).intValue() != sol.getTable().get(vm).intValue()) {

				toSwap.add(vm);
			}
		}

		// remove phase
		for (Container v : toSwap) {
			stubs.get(this.sol.getTable().get(v).intValue()).remove(v, stubs, this.sol, dc);
			this.sol.getTable().remove(v);
		} // allocate phase
		for (Container v : toSwap) {
			int tmp = sol.getTable().get(v).intValue();
			stubs.get(tmp).allocate(v, stubs, this.sol, dc, true);
			this.sol.getTable().put(v, new Integer(tmp));
		}

		this.sol = (CPPSolution) sol.clone();

		if (allconts.size() != sol.getTable().size()) {
			allconts = new ArrayList<Container>();
			allconts.addAll(sol.getTable().keySet());
		}

		cust_index = 0;
		conts = custs.get(cust_index).getNewContainers();
		copy = (CPPSolution) this.sol.clone();
		stubs.get(sol.getTable().get(conts.get(index_one)).intValue()).remove(conts.get(index_one), stubs, copy, dc);
		copy.getTable().remove(conts.get(index_one));
		deltacurrent = deltaObj(conts.get(index_one),
				stubs.get(this.sol.getTable().get(conts.get(index_one)).intValue()), copy, false);

	}

	@Override
	public CPPSolution next() {
		index_two += 1;
		if (index_two >= conts.size()) {
			stubs.get(sol.getTable().get(conts.get(index_one)).intValue()).allocate(conts.get(index_one), stubs, copy,
					dc, true);
			copy.getTable().put(conts.get(index_one), sol.getTable().get(conts.get(index_one)));

			index_one += 1;
			index_two = index_one + 1;

			if (index_one >= conts.size() - 1) {
				cust_index++;
				index_one = 0;
				index_two = 1;
				updateCustomer();
			}

			stubs.get(sol.getTable().get(conts.get(index_one)).intValue()).remove(conts.get(index_one), stubs, copy,
					dc);
			copy.getTable().remove(conts.get(index_one));
			deltacurrent = deltaObj(conts.get(index_one),
					stubs.get(this.sol.getTable().get(conts.get(index_one)).intValue()), copy, false);

		}

		return generateSolution();
	}

	protected void updateCustomer() {
		conts = custs.get(cust_index).getNewContainers();

	}

	@Override
	public void clear() {
		super.clear();
		allconts.clear();

	}

}
