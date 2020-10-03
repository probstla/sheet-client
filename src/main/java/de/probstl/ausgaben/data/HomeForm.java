package de.probstl.ausgaben.data;

/**
 * Form for storing selection in Landing page
 */
public class HomeForm {

	/** The selected month to continue */
	private String m_SelectedMonth;

	/**
	 * @return the selectedMonth
	 */
	public String getSelectedMonth() {
		return m_SelectedMonth;
	}

	/**
	 * @param selectedMonth The selected month
	 */
	public void setSelectedMonth(String selectedMonth) {
		m_SelectedMonth = selectedMonth;
	}
}
