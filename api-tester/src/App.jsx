import { useEffect, useState, useCallback } from 'react';
import TopBar from './components/TopBar.jsx';
import NavRail from './components/NavRail.jsx';
import StudentScreen from './screens/StudentScreen.jsx';
import StaffScreen from './screens/StaffScreen.jsx';
import AcademicsScreen from './screens/AcademicsScreen.jsx';
import FinanceScreen from './screens/FinanceScreen.jsx';
import CrmScreen from './screens/CrmScreen.jsx';
import GuardiansScreen from './screens/GuardiansScreen.jsx';
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

          {activeTab === 'students' && (
            <StudentScreen
              schoolId={schoolId}
              years={years}
              year={year}
              reload={handleReload}
            />
          )}

          {activeTab === 'staff' && (
            <StaffScreen
              schoolId={schoolId}
              reload={handleReload}
            />
          )}

          {activeTab === 'academics' && (
            <AcademicsScreen
              schoolId={schoolId}
              years={years}
              year={year}
              staff={staff}
              reload={handleReload}
            />
          )}

          {activeTab === 'finance' && (
            <FinanceScreen
              schoolId={schoolId}
              years={years}
              year={year}
              reload={handleReload}
            />
          )}

          {activeTab === 'crm' && (
            <CrmScreen
              schoolId={schoolId}
              year={year}
              staff={staff}
            />
          )}

          {activeTab === 'guardians' && (
            <GuardiansScreen schoolId={schoolId} />
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
