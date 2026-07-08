import { useEffect, useState, useCallback } from 'react';
import TopBar from './components/TopBar.jsx';
import NavRail from './components/NavRail.jsx';
import SetupScreen from './screens/SetupScreen.jsx';
import ClassesScreen from './screens/ClassesScreen.jsx';
import TimetableBuilder from './screens/TimetableBuilder.jsx';
import TimetableView from './screens/TimetableView.jsx';
import CoreScreen from './screens/CoreScreen.jsx';
import { api } from './api.js';
import { ToastProvider } from './components/ui.jsx';

function AppContent() {
  const [schools, setSchools] = useState([]);
  const [schoolId, setSchoolId] = useState('');
  const [years, setYears] = useState([]);
  const [year, setYear] = useState('');
  const [classes, setClasses] = useState([]);
  const [staff, setStaff] = useState([]);
  const [activeTab, setActiveTab] = useState('core');
  const [timetableSubTab, setTimetableSubTab] = useState('view');

  // Load initial schools
  useEffect(() => {
    api.schools()
      .then((data) => {
        setSchools(data);
        if (data.length > 0) {
          setSchoolId(data[0].id);
        }
      })
      .catch((err) => console.error("Failed to load schools:", err));
  }, []);

  // Fetch school-specific data
  const loadSchoolData = useCallback(async (sid) => {
    if (!sid) {
      setYears([]);
      setClasses([]);
      setStaff([]);
      setYear('');
      return;
    }

    try {
      const [ys, cls, st] = await Promise.all([
        api.academicYears(sid),
        api.classes(sid),
        api.staff(sid),
      ]);
      setYears(ys);
      setClasses(cls);
      setStaff(st);

      // Auto-select a year
      if (ys.length > 0) {
        // Try to keep currently selected year if it exists in the new list,
        // otherwise select current date match or first year.
        const currentYearExists = ys.some((y) => y.name === year);
        if (!currentYearExists) {
          const todayStr = new Date().toISOString().slice(0, 10);
          const cur = ys.find((y) => y.startDate <= todayStr && todayStr <= y.endDate) || ys[0];
          setYear(cur.name);
        }
      } else {
        setYear('');
      }
    } catch (err) {
      console.error("Failed to load school data:", err);
    }
  }, [year]);

  useEffect(() => {
    loadSchoolData(schoolId);
  }, [schoolId]);

  // Handle manual reload from screens
  const handleReload = useCallback(async (targetYearName) => {
    if (!schoolId) return;
    try {
      const [ys, cls, st] = await Promise.all([
        api.academicYears(schoolId),
        api.classes(schoolId),
        api.staff(schoolId),
      ]);
      setYears(ys);
      setClasses(cls);
      setStaff(st);
      if (targetYearName) {
        setYear(targetYearName);
      }
    } catch (err) {
      console.error("Failed to reload data:", err);
    }
  }, [schoolId]);

  return (
    <div className="h-screen flex flex-col overflow-hidden bg-slate-50">
      <TopBar
        schools={schools}
        schoolId={schoolId}
        onSchool={setSchoolId}
        years={years}
        year={year}
        onYear={setYear}
      />
      <div className="flex flex-1 min-h-0">
        <NavRail active={activeTab} onChange={setActiveTab} />
        <main className="flex-1 overflow-y-auto p-6">
          {activeTab === 'setup' && (
            <SetupScreen
              schoolId={schoolId}
              years={years}
              year={year}
              reload={handleReload}
            />
          )}
          {activeTab === 'classes' && (
            <ClassesScreen
              schoolId={schoolId}
              year={year}
            />
          )}
          {activeTab === 'timetable' && (
            <div className="flex flex-col h-full gap-4">
              <div className="flex border-b border-slate-200 bg-white px-4 pt-2 rounded-t-xl shadow-sm">
                <button
                  onClick={() => setTimetableSubTab('view')}
                  className={`px-4 py-2 text-sm font-semibold border-b-2 transition-colors -mb-px ${timetableSubTab === 'view' ? 'border-blue-600 text-blue-600' : 'border-transparent text-slate-500 hover:text-slate-700'}`}
                >
                  View Schedule
                </button>
                <button
                  onClick={() => setTimetableSubTab('build')}
                  className={`px-4 py-2 text-sm font-semibold border-b-2 transition-colors -mb-px ${timetableSubTab === 'build' ? 'border-blue-600 text-blue-600' : 'border-transparent text-slate-500 hover:text-slate-700'}`}
                >
                  Build Timetable
                </button>
              </div>
              <div className="flex-1">
                {timetableSubTab === 'view' ? (
                  <TimetableView
                    schoolId={schoolId}
                    classes={classes}
                    staff={staff}
                  />
                ) : (
                  <TimetableBuilder
                    schoolId={schoolId}
                    year={year}
                    yearDoc={years.find((y) => y.name === year)}
                    classes={classes}
                    staff={staff}
                  />
                )}
              </div>
            </div>
          )}
          {activeTab === 'core' && (
            <CoreScreen
              schoolId={schoolId}
              schools={schools}
              year={year}
              years={years}
              reload={handleReload}
            />
          )}
        </main>
      </div>
    </div>
  );
}

export default function App() {
  return (
    <ToastProvider>
      <AppContent />
    </ToastProvider>
  );
}
