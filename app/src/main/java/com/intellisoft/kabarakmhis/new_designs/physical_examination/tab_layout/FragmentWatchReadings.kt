package com.intellisoft.kabarakmhis.new_designs.physical_examination.tab_layout

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.fhir.FhirEngine
import com.intellisoft.kabarakmhis.R
import com.intellisoft.kabarakmhis.fhir.FhirApplication
import com.intellisoft.kabarakmhis.fhir.viewmodels.MainActivityViewModel
import com.intellisoft.kabarakmhis.fhir.viewmodels.PatientDetailsViewModel
import com.intellisoft.kabarakmhis.helperclass.*
import com.intellisoft.kabarakmhis.new_designs.data_class.DbResourceViews
import kotlinx.android.synthetic.main.fragment_smart_reading.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FragmentWatchReadings : Fragment() {

    private lateinit var rootView: View
    private lateinit var patientDetailsViewModel: PatientDetailsViewModel
    private lateinit var patientId: String
    private lateinit var fhirEngine: FhirEngine
    private val mainViewModel: MainActivityViewModel by viewModels()
    private val formatterClass = FormatterClass()

    private lateinit var layoutManager: RecyclerView.LayoutManager

    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_smart_reading, container, false)

        mainViewModel.poll()

        patientId = formatterClass.retrieveSharedPreference(requireContext(), "patientId").toString()
        fhirEngine = FhirApplication.fhirEngine(requireContext())

        patientDetailsViewModel = ViewModelProvider(this,
            PatientDetailsViewModel.
            PatientDetailsViewModelFactory(requireActivity().application,fhirEngine, patientId)
        )[PatientDetailsViewModel::class.java]

        recyclerView = rootView.findViewById(R.id.recyclerView);
        layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL,
            false
        )
        recyclerView.layoutManager = layoutManager
        recyclerView.setHasFixedSize(true)

        return rootView
    }

    override fun onStart() {
        super.onStart()

        CoroutineScope(Dispatchers.IO).launch {

            val smartWatchReadingList = patientDetailsViewModel.getObservationFromEncounter(DbObservationValues.CLIENT_WEARABLE_RECORDING.name)
            if (smartWatchReadingList.isNotEmpty()){

                //Get Observations

                val id = smartWatchReadingList[0].id


                val observationList = patientDetailsViewModel.getObservationsFromEncounter(id)
                val dbWatchDataList = ArrayList<DbWatchData>()

                observationList.groupBy { it.issued }.forEach { (issued, observationItems) ->

                    //Get the group date and convert to human readable date
                    val issuedDate = issued.toString()
                    val newDate = FormatterClass().convertFhirDate(issuedDate)

                    val dbWatchDataValuesList = ArrayList<DbWatchDataValues>()

                    //Get the observations for the group date
                    observationItems.forEach {

                        val time = FormatterClass().convertFhirTime(issuedDate)

                        val dbSmartWatchReading = DbWatchDataValues(time = time.toString(), text = it.text, value = it.value)
                        dbWatchDataValuesList.add(dbSmartWatchReading)

                    }

                    val dbWatchData = DbWatchData(date = newDate.toString(), readings = dbWatchDataValuesList)
                    dbWatchDataList.add(dbWatchData)

                }



                CoroutineScope(Dispatchers.Main).launch {

                    if (observationList.isNotEmpty()){
                        no_record.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                    }else{
                        no_record.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    }

                    val configurationListingAdapter = SmartWatchReadingAdapter(
                        dbWatchDataList,requireContext())
                    recyclerView.adapter = configurationListingAdapter

                }



            }

        }




    }

}
