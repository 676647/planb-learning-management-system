const STORAGE_KEY = 'lms_courses_data';

// Hilfsfunktion: Lädt Daten aus dem LocalStorage
const loadFromStorage = () => {
    const data = localStorage.getItem(STORAGE_KEY);
    return data ? JSON.parse(data) : [];
};

// Hilfsfunktion: Speichert Daten im LocalStorage
const saveToStorage = (courses) => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(courses));
};

export const courseService = {
    // Alle Kurse abrufen (simuliert async API-Call)
    async getCourses() {
        return new Promise((resolve) => {
            setTimeout(() => resolve(loadFromStorage()), 200); // 200ms API-Verzögerung simulieren
        });
    },

    // Einzelnen Kurs nach ID abrufen
    async getCourseById(id) {
        const courses = loadFromStorage();
        return courses.find(c => c.id === Number(id)) || null;
    },

    // Neuen Kurs erstellen
    async createCourse(courseData) {
        const courses = loadFromStorage();
        const newCourse = {
            id: Date.now(),
            title: courseData.title,
            semester: courseData.semester || 'Autumn 2026',
            description: courseData.description || '',
            modules: []
        };
        courses.push(newCourse);
        saveToStorage(courses);
        return newCourse;
    },

    // Modul zu einem Kurs hinzufügen
    async addModuleToCourse(courseId, moduleTitle, moduleDescription) {
        const courses = loadFromStorage();
        const course = courses.find(c => c.id === Number(courseId));
        if (!course) return null;

        const newModule = {
            id: Date.now(),
            title: moduleTitle,
            description: moduleDescription,
            items: []
        };
        course.modules.push(newModule);
        saveToStorage(courses);
        return newModule;
    },

    // Inhalt (Page/Quiz/Coding/Assignment) zu einem Modul hinzufügen
    async addItemToModule(courseId, moduleId, itemData) {
        const courses = loadFromStorage();
        const course = courses.find(c => c.id === Number(courseId));
        if (!course) return null;

        const module = course.modules.find(m => m.id === Number(moduleId));
        if (!module) return null;

        const newItem = {
            id: Date.now(),
            ...itemData
        };
        module.items.push(newItem);
        saveToStorage(courses);
        return newItem;
    },

    // Kurs löschen
    async deleteCourse(courseId) {
        let courses = loadFromStorage();
        courses = courses.filter(c => c.id !== Number(courseId));
        saveToStorage(courses);
        return true;
    }
};