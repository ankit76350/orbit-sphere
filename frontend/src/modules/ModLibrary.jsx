/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */
import { useState, useEffect } from "react";
import { getBooks, saveBooks, logAction } from "../storage";
import { Button, Input, Select, Dialog, Badge, useToast } from "../components/ui";
import {
  Book,
  BookOpen,
  Search,
  Plus,
  Download,
  Maximize2,
  Minimize2,
  ZoomIn,
  ZoomOut,
  Sun,
  Moon,
  Bookmark,
  ChevronLeft,
  ChevronRight,
  Eye,
  Trash2,
  Filter,
  Check
} from "lucide-react";

// Predefined detailed content pages for standard seeded books
const bookPagesContent = {
  "book-1": [
    { page: 1, title: "Cover Page", content: "ADVANCED ALGEBRAIC STRUCTURES & FUNCTIONS\n\nEdition 2026\nSt. Jude's Boarding Academy Press\n\nAuthor: Dr. Elizabeth Sharma\n\nThis textbook is optimized for high school secondary preparation." },
    { page: 2, title: "Table of Contents", content: "CHAPTER 1: Linear Systems & Matrices ......... Page 3\nCHAPTER 2: Vector Spaces & Linear Maps ........ Page 5\nCHAPTER 3: Complex Transformations .......... Page 7\nCHAPTER 4: Analytical Trigonometry ........... Page 9" },
    { page: 3, title: "1.1 Introduction to Matrices", content: "A matrix is a rectangular array of numbers arranged in rows and columns. In mathematics, linear equations are often solved using matrices.\n\nLet A be a matrix of size m x n:\n  A = [a_ij]\nwhere i ranges from 1 to m, and j ranges from 1 to n.\n\nMatrix multiplication is associative, but generally non-commutative (AB != BA)." },
    { page: 4, title: "1.2 Determinants & Inverses", content: "For a square matrix A, the determinant det(A) determines if the matrix has an inverse. A matrix A is invertible if and only if det(A) != 0.\n\nThe inverse is given by:\n  A^-1 = 1/det(A) * adj(A)\n\nWhere adj(A) is the classical adjoint matrix." },
    { page: 5, title: "2.1 Definition of Vector Spaces", content: "A vector space (also called a linear space) is a collection of objects called vectors, which may be added together and multiplied by numbers (scalars).\n\nTen axioms must be satisfied, including associativity of addition, commutativity of addition, identity element of addition, and distributivity." },
    { page: 6, title: "2.2 Subspaces & Basis Vectors", content: "A subset W of a vector space V is a subspace if it contains the zero vector, is closed under addition, and is closed under scalar multiplication.\n\nA basis B of V is a linearly independent subset of V that spans V. The dimension dim(V) is the cardinality of B." }
  ],
  "book-2": [
    { page: 1, title: "Cover Page", content: "INORGANIC CHEMISTRY: PRINCIPLES & LAB PROTOCOLS\n\nSt. Jude's Secondary Chemistry Series\n\nAuthor: Prof. Diya Verma\n\nApproved for chemistry laboratory modules." },
    { page: 2, title: "Table of Contents", content: "CHAPTER 1: Transition Metal Compounds ........ Page 3\nCHAPTER 2: Catalysis & Complex Kinetics ....... Page 5\nCHAPTER 3: Lab Safety & Reagent Controls ...... Page 7" },
    { page: 3, title: "1.1 The Transition Elements", content: "Transition metals occupy groups 3 through 12 of the periodic table. They are characterized by the filling of an inner d electron shell.\n\nProperties include:\n  - Multiple oxidation states (e.g. Fe2+ vs Fe3+)\n  - Formation of highly colored coordination complex ions\n  - Paramagnetic behavior due to unpaired d-electrons." },
    { page: 4, title: "1.2 Coordination Number & Ligands", content: "Ligands are molecules or ions that donate a pair of electrons to a central metal atom to form a coordinate covalent bond.\n\nCommon ligands include:\n  - Monodentate: H2O, NH3, Cl-\n  - Bidentate: Ethylenediamine (en), Oxalate (ox)\n  - Polydentate: EDTA (hexadentate, used in complexometric titrations)." }
  ],
  "book-4": [
    { page: 1, title: "Cover Page", content: "INTRODUCTION TO COMPUTER SCIENCE & ALGORITHMS\n\nFoundational Principles of Software Engineering\n\nAuthor: Prof. Chloe Smith\n\nOptimized for Grade 9-12 Computer Science Cohorts." },
    { page: 2, title: "Table of Contents", content: "CHAPTER 1: Time Complexity & Big-O ........... Page 3\nCHAPTER 2: Essential Sorting Algorithms ......... Page 5\nCHAPTER 3: Object-Oriented Design Systems ..... Page 7" },
    { page: 3, title: "1.1 Algorithmic Performance", content: "In computer science, time complexity is the computational complexity that describes the amount of computer time it takes to run an algorithm.\n\nTime complexity is commonly estimated by counting the number of elementary operations performed, using Big-O notation:\n  - O(1): Constant Time\n  - O(log n): Logarithmic Time (Binary Search)\n  - O(n): Linear Time\n  - O(n log n): Quick Sort, Merge Sort" },
    { page: 4, title: "1.2 Space Complexity & Memory", content: "Space complexity refers to the total amount of memory space that an algorithm or program uses during its execution relative to the input size.\n\nOptimizing algorithms requires balancing the space-time tradeoff (e.g., using hashing tables to gain O(1) lookups at the expense of O(n) auxiliary memory)." }
  ]
};

// Generate fallback content for custom added books
const getBookPages = (book) => {
  if (bookPagesContent[book.id]) {
    return bookPagesContent[book.id];
  }
  const pages = [
    { page: 1, title: "Cover Page", content: `${book.title.toUpperCase()}\n\nCatalog ID: ${book.id}\nAuthor: ${book.author}\nCategory: ${book.category}\nISBN: ${book.isbn || "N/A"}\n\nPublished in Year ${book.publishedYear || 2026}\n\nSt. Jude's Boarding Academy Digital Repositories` },
    { page: 2, title: "Introduction & Context", content: `INTRODUCTION\n\n${book.description || "No description provided."}\n\nThis volume is digitized under St. Jude's Boarding Library protocols. Access is granted to students, staff and parent cohorts.` }
  ];
  for (let p = 3; p <= 8; p++) {
    pages.push({
      page: p,
      title: `Chapter ${p - 2}: Core Concepts in ${book.category}`,
      content: `Section ${p - 2}.1 - Analytical Discussions\n\nThis page covers the advanced details of ${book.category} and how it applies to ${book.grade || "all grades"}.\n\nResearchers and educators suggest that mastering these topics builds a strong foundation for tertiary studies. Please discuss with your course tutor Prof. ${book.author} or reference associated literature databases.`
    });
  }
  return pages;
};

export default function ModLibrary({ user }) {
  const { addToast } = useToast();
  const [books, setBooks] = useState(() => getBooks());
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedCategory, setSelectedCategory] = useState("All");
  const [selectedGrade, setSelectedGrade] = useState("All");
  
  // Modals state
  const [isUploadOpen, setIsUploadOpen] = useState(false);
  const [isReaderOpen, setIsReaderOpen] = useState(false);
  
  // Selected book to read
  const [activeBook, setActiveBook] = useState(null);
  const [activePageNum, setActivePageNum] = useState(1);
  const [zoomLevel, setZoomLevel] = useState(100);
  const [readerTheme, setReaderTheme] = useState("light"); // light, dark, sepia
  const [pdfSearchTerm, setPdfSearchTerm] = useState("");
  const [bookmarks, setBookmarks] = useState({}); // bookId -> array of page numbers

  // Upload form state
  const [newTitle, setNewTitle] = useState("");
  const [newAuthor, setNewAuthor] = useState("");
  const [newCategory, setNewCategory] = useState("Mathematics");
  const [newGrade, setNewGrade] = useState("All Grades");
  const [newIsbn, setNewIsbn] = useState("");
  const [newPublishedYear, setNewPublishedYear] = useState("2026");
  const [newPages, setNewPages] = useState("120");
  const [newGradient, setNewGradient] = useState("from-blue-600 to-indigo-800");
  const [newDescription, setNewDescription] = useState("");

  const categories = ["All", "Mathematics", "Science", "History", "Literature", "Computer Science"];
  const gradientOptions = [
    { label: "Ocean Breeze (Blue)", value: "from-blue-600 to-indigo-800" },
    { label: "Emerald Gate (Green)", value: "from-emerald-500 to-teal-700" },
    { label: "Sunset Glow (Orange)", value: "from-amber-600 to-orange-850" },
    { label: "Royal Plum (Purple)", value: "from-indigo-600 to-purple-800" },
    { label: "Rose Petal (Pink)", value: "from-rose-500 to-pink-700" },
    { label: "Forest Moss (Deep Teal)", value: "from-teal-600 to-emerald-800" },
    { label: "Midnight Carbon (Dark)", value: "from-slate-700 to-slate-900" }
  ];

  // Sync bookmarks from localStorage if available
  useEffect(() => {
    const savedBookmarks = localStorage.getItem("erp_library_bookmarks");
    if (savedBookmarks) {
      try {
        setBookmarks(JSON.parse(savedBookmarks));
      } catch (e) {
        console.error(e);
      }
    }
  }, []);

  const handleToggleBookmark = (bookId, page) => {
    const list = bookmarks[bookId] || [];
    let updated;
    if (list.includes(page)) {
      updated = list.filter((p) => p !== page);
      addToast("Bookmark Removed", `Removed page ${page} from bookmarks.`);
    } else {
      updated = [...list, page].sort((a, b) => a - b);
      addToast("Bookmark Added", `Page ${page} saved.`);
    }
    const next = { ...bookmarks, [bookId]: updated };
    setBookmarks(next);
    localStorage.setItem("erp_library_bookmarks", JSON.stringify(next));
  };

  const handleCreateBook = (e) => {
    e.preventDefault();
    if (!newTitle || !newAuthor) {
      addToast("Error", "Book Title and Author are required", "error");
      return;
    }
    
    const newBook = {
      id: `book-${Date.now()}`,
      title: newTitle,
      author: newAuthor,
      category: newCategory,
      grade: newGrade,
      isbn: newIsbn || `978-${Math.floor(1e9 + Math.random() * 9e9)}`,
      publishedYear: parseInt(newPublishedYear) || 2026,
      pages: parseInt(newPages) || 100,
      coverGradient: newGradient,
      pdfUrl: "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
      description: newDescription || "A digital repository textbook."
    };

    const updated = [newBook, ...books];
    setBooks(updated);
    saveBooks(updated);
    logAction(user.id, user.name, user.role, "Library Book Registered", `Catalogued new PDF book: "${newTitle}" by ${newAuthor}`);
    addToast("Success", `"${newTitle}" catalogued successfully!`);
    
    // Reset Form
    setNewTitle("");
    setNewAuthor("");
    setNewIsbn("");
    setNewDescription("");
    setIsUploadOpen(false);
  };

  const handleDeleteBook = (bookId, title) => {
    if (window.confirm(`Are you sure you want to remove "${title}" from the catalog?`)) {
      const updated = books.filter((b) => b.id !== bookId);
      setBooks(updated);
      saveBooks(updated);
      logAction(user.id, user.name, user.role, "Library Book Removed", `Deleted catalog book ID ${bookId}: "${title}"`);
      addToast("Book Deleted", `"${title}" removed from catalog.`);
    }
  };

  const handleOpenReader = (book) => {
    setActiveBook(book);
    setActivePageNum(1);
    setIsReaderOpen(true);
    setPdfSearchTerm("");
  };

  const handleDownloadPdf = (book) => {
    // Generate a quick downloadable mock text file to simulate PDF download
    const element = document.createElement("a");
    const pages = getBookPages(book);
    const textData = pages.map(p => `--- PAGE ${p.page}: ${p.title} ---\n\n${p.content}\n`).join("\n");
    const file = new Blob([textData], { type: "text/plain" });
    element.href = URL.createObjectURL(file);
    element.download = `${book.title.replace(/\s+/g, "_")}_Manual.txt`;
    document.body.appendChild(element);
    element.click();
    document.body.removeChild(element);
    
    logAction(user.id, user.name, user.role, "Library Document Downloaded", `Downloaded digital text compilation for "${book.title}"`);
    addToast("Download Started", `Downloading compilation file for "${book.title}"`);
  };

  // Filter books list
  const filteredBooks = books.filter((b) => {
    const matchesSearch =
      b.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
      b.author.toLowerCase().includes(searchTerm.toLowerCase()) ||
      b.isbn.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesCategory = selectedCategory === "All" || b.category === selectedCategory;
    const matchesGrade = selectedGrade === "All" || b.grade === selectedGrade;
    return matchesSearch && matchesCategory && matchesGrade;
  });

  const canManageCatalog = user.role === "Super Admin" || user.role === "Principal";
  
  const currentPages = activeBook ? getBookPages(activeBook) : [];
  const currentPage = currentPages.find(p => p.page === activePageNum) || currentPages[0] || { page: 1, title: "", content: "" };

  // Helper to highlight terms in mock content
  const highlightSearch = (text, term) => {
    if (!term) return text;
    const parts = text.split(new RegExp(`(${term})`, "gi"));
    return parts.map((part, idx) => 
      part.toLowerCase() === term.toLowerCase() 
        ? <span key={idx} className="bg-yellow-300 text-slate-900 px-0.5 rounded font-bold">{part}</span>
        : part
    );
  };

  return (
    <div className="space-y-6">
      {/* Title Header banner */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 bg-white p-5 rounded-3xl border border-slate-100 shadow-xs relative overflow-hidden">
        <div className="absolute right-0 top-0 -mt-12 -mr-12 h-36 w-36 bg-blue-500 rounded-full blur-3xl opacity-10 pointer-events-none" />
        <div>
          <h2 className="text-xl font-extrabold text-slate-800 tracking-tight flex items-center gap-2">
            <Book className="h-5.5 w-5.5 text-blue-600" />
            Campus Library & PDF Repository
          </h2>
          <p className="text-xs text-slate-400 font-semibold mt-1">
            Browse secondary education digital textbooks, lab guides, and literature publications. Reading state preserves bookmarks.
          </p>
        </div>

        {canManageCatalog && (
          <Button onClick={() => setIsUploadOpen(true)} className="text-xs py-2 bg-slate-900 border border-transparent shadow-xs">
            <Plus className="h-4 w-4" /> Catalog Book
          </Button>
        )}
      </div>

      {/* Filter and search block */}
      <div className="bg-white border border-slate-100 p-5 rounded-3xl space-y-4 shadow-xs">
        <div className="flex flex-col md:flex-row gap-4 items-center justify-between">
          {/* Tabs category filter */}
          <div className="flex gap-1.5 bg-slate-100 p-1 rounded-2xl w-full md:w-auto overflow-x-auto scrollbar-none self-stretch md:self-auto shrink-0">
            {categories.map((cat) => (
              <button
                key={cat}
                onClick={() => setSelectedCategory(cat)}
                className={`px-3.5 py-1.5 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${
                  selectedCategory === cat ? "bg-white text-blue-700 shadow-xs" : "text-slate-500 hover:text-slate-805"
                }`}
              >
                {cat}
              </button>
            ))}
          </div>

          {/* Grade selection & search */}
          <div className="flex gap-3 w-full md:w-auto items-center">
            <div className="w-40 shrink-0">
              <Select
                options={[
                  { label: "All Grades", value: "All" },
                  { label: "Grade 7", value: "Grade 7" },
                  { label: "Grade 8", value: "Grade 8" },
                  { label: "Grade 9", value: "Grade 9" },
                  { label: "Grade 10", value: "Grade 10" },
                  { label: "Grade 12", value: "Grade 12" }
                ]}
                value={selectedGrade}
                onChange={(e) => setSelectedGrade(e.target.value)}
                className="text-xs py-1.5 h-8.5 rounded-xl bg-slate-50"
              />
            </div>
            
            <div className="relative w-full md:w-64">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-slate-400" />
              <input
                type="text"
                placeholder="Search catalog..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="bg-slate-50 border border-slate-200 rounded-xl pl-9.5 pr-4 py-1.5 text-xs font-semibold focus:outline-none focus:ring-2 focus:ring-blue-500 w-full transition"
              />
            </div>
          </div>
        </div>
      </div>

      {/* Books grid */}
      {filteredBooks.length === 0 ? (
        <div className="bg-white border border-slate-100 p-12 text-center rounded-3xl">
          <BookOpen className="h-12 w-12 text-slate-350 mx-auto mb-3" />
          <h4 className="text-sm font-bold text-slate-700">No Catalog Entries Found</h4>
          <p className="text-xs text-slate-400 mt-1 max-w-sm mx-auto">
            Try adjusting your search criteria or subject categories to discover books in the digital library.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 xl:grid-cols-4 gap-6">
          {filteredBooks.map((book) => (
            <div key={book.id} className="bg-white border border-slate-100 rounded-3xl overflow-hidden shadow-xs hover:shadow-md transition group flex flex-col justify-between h-[390px]">
              
              {/* Cover jacket visual representation */}
              <div className={`h-48 bg-gradient-to-br ${book.coverGradient} p-5 text-white flex flex-col justify-between relative overflow-hidden shrink-0`}>
                <div className="absolute top-0 right-0 -mt-8 -mr-8 h-28 w-28 bg-white/10 rounded-full blur-xl pointer-events-none" />
                
                <div className="flex justify-between items-start z-10">
                  <Badge className="bg-white/20 border-white/10 text-white text-[9px] font-black tracking-wide uppercase px-2 py-0.5 rounded-md backdrop-blur-xs">
                    {book.category}
                  </Badge>
                  
                  {canManageCatalog && (
                    <button
                      onClick={() => handleDeleteBook(book.id, book.title)}
                      className="p-1.5 bg-white/10 hover:bg-rose-600/90 text-white rounded-lg transition"
                      title="Remove from Catalog"
                    >
                      <Trash2 className="h-3.5 w-3.5" />
                    </button>
                  )}
                </div>

                <div className="space-y-1 z-10">
                  <h3 className="text-sm font-extrabold font-serif leading-tight tracking-wide line-clamp-2">
                    {book.title}
                  </h3>
                  <p className="text-[10px] text-white/80 font-semibold italic">
                    By {book.author}
                  </p>
                </div>

                <div className="flex justify-between items-center z-10 border-t border-white/10 pt-2 text-[9px] font-black uppercase tracking-wider text-white/90 font-mono">
                  <span>{book.grade}</span>
                  <span>{book.pages} Pages</span>
                </div>
              </div>

              {/* Card metadata detail */}
              <div className="p-5 flex-1 flex flex-col justify-between space-y-3.5">
                <div>
                  <p className="text-[11px] text-slate-400 font-semibold line-clamp-3 leading-relaxed">
                    {book.description}
                  </p>
                </div>

                <div className="pt-2 border-t border-slate-50 flex gap-2">
                  <Button
                    onClick={() => handleOpenReader(book)}
                    className="flex-1 text-xs py-2 bg-blue-50 border border-blue-100 text-blue-750 hover:bg-blue-100 active:scale-[0.98] cursor-pointer flex gap-1.5"
                  >
                    <Eye className="h-3.5 w-3.5" /> Read PDF
                  </Button>
                  
                  <Button
                    onClick={() => handleDownloadPdf(book)}
                    variant="outline"
                    className="p-2 shrink-0 border-slate-200 hover:bg-slate-50 cursor-pointer"
                    title="Download Book compiled text file"
                  >
                    <Download className="h-4 w-4 text-slate-500" />
                  </Button>
                </div>
              </div>

            </div>
          ))}
        </div>
      )}

      {/* 1. INTERACTIVE MOCK PDF READER DIALOG */}
      <Dialog
        isOpen={isReaderOpen}
        onClose={() => setIsReaderOpen(false)}
        title={activeBook ? `PDF Viewer - ${activeBook.title}` : "PDF Viewer"}
        maxWidth="max-w-6xl"
      >
        {activeBook && (
          <div className="flex flex-col md:flex-row h-[75vh] -mx-6 -my-4 border-t border-slate-100">
            
            {/* Sidebar Table of Contents (TOC) */}
            <aside className="w-full md:w-60 bg-slate-50 border-r border-slate-100 flex flex-col justify-between shrink-0 overflow-y-auto">
              <div className="p-4 space-y-4">
                <div>
                  <h4 className="text-[10px] uppercase font-bold text-slate-400 tracking-wider">Book Index (TOC)</h4>
                  <div className="space-y-1.5 mt-3">
                    {currentPages.map((p) => {
                      const isActive = p.page === activePageNum;
                      return (
                        <button
                          key={p.page}
                          onClick={() => setActivePageNum(p.page)}
                          className={`w-full text-left px-2.5 py-1.5 text-xs font-bold rounded-lg block transition truncate ${
                            isActive 
                              ? "bg-blue-600 text-white" 
                              : "hover:bg-slate-100 text-slate-655 hover:text-slate-800"
                          }`}
                        >
                          <span className="font-mono text-[10px] mr-1.5 opacity-80">{p.page}</span>
                          {p.title}
                        </button>
                      );
                    })}
                  </div>
                </div>

                <div className="border-t border-slate-200 pt-4">
                  <h4 className="text-[10px] uppercase font-bold text-slate-400 tracking-wider">My Saved Bookmarks</h4>
                  <div className="space-y-1 mt-2.5">
                    {(!bookmarks[activeBook.id] || bookmarks[activeBook.id].length === 0) ? (
                      <p className="text-[10px] text-slate-400 italic">No bookmarks on this volume</p>
                    ) : (
                      bookmarks[activeBook.id].map((pNum) => (
                        <button
                          key={pNum}
                          onClick={() => setActivePageNum(pNum)}
                          className="w-full flex items-center justify-between text-left px-2.5 py-1 rounded-md text-[11px] font-semibold text-slate-600 hover:bg-slate-150 transition"
                        >
                          <span>Page {pNum}</span>
                          <Bookmark className="h-3 w-3 text-amber-500 fill-amber-500" />
                        </button>
                      ))
                    )}
                  </div>
                </div>
              </div>

              <div className="p-4 border-t border-slate-150 bg-slate-100 text-[9px] font-bold font-mono text-slate-450 tracking-wider">
                ISBN: {activeBook.isbn}
              </div>
            </aside>

            {/* Main Document Reader Console */}
            <div className="flex-1 flex flex-col bg-slate-100 overflow-hidden">
              
              {/* Toolbar */}
              <div className="bg-white border-b border-slate-200 px-4 py-2.5 flex flex-wrap gap-3 items-center justify-between shrink-0">
                {/* Search inside PDF */}
                <div className="relative w-44">
                  <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-slate-400" />
                  <input
                    type="text"
                    placeholder="Search inside page..."
                    value={pdfSearchTerm}
                    onChange={(e) => setPdfSearchTerm(e.target.value)}
                    className="bg-slate-50 border border-slate-200 rounded-lg pl-8 pr-2.5 py-1 text-[11px] font-bold focus:outline-none focus:ring-1 focus:ring-blue-500 w-full"
                  />
                </div>

                {/* Page Navigation */}
                <div className="flex items-center gap-2">
                  <button
                    disabled={activePageNum === 1}
                    onClick={() => setActivePageNum(p => Math.max(1, p - 1))}
                    className="p-1 hover:bg-slate-100 active:scale-[0.95] disabled:opacity-40 disabled:pointer-events-none rounded-lg text-slate-500 transition"
                  >
                    <ChevronLeft className="h-4.5 w-4.5" />
                  </button>
                  <span className="text-xs font-bold text-slate-700 font-mono whitespace-nowrap">
                    Page {activePageNum} / {currentPages.length}
                  </span>
                  <button
                    disabled={activePageNum === currentPages.length}
                    onClick={() => setActivePageNum(p => Math.min(currentPages.length, p + 1))}
                    className="p-1 hover:bg-slate-100 active:scale-[0.95] disabled:opacity-40 disabled:pointer-events-none rounded-lg text-slate-500 transition"
                  >
                    <ChevronRight className="h-4.5 w-4.5" />
                  </button>
                </div>

                {/* Zoom controls */}
                <div className="flex items-center gap-1.5">
                  <button
                    disabled={zoomLevel <= 75}
                    onClick={() => setZoomLevel(z => Math.max(75, z - 25))}
                    className="p-1 hover:bg-slate-100 rounded-lg text-slate-500 transition"
                    title="Zoom Out"
                  >
                    <ZoomOut className="h-4 w-4" />
                  </button>
                  <span className="text-xs font-mono font-bold text-slate-655 min-w-[36px] text-center">
                    {zoomLevel}%
                  </span>
                  <button
                    disabled={zoomLevel >= 175}
                    onClick={() => setZoomLevel(z => Math.min(175, z + 25))}
                    className="p-1 hover:bg-slate-100 rounded-lg text-slate-500 transition"
                    title="Zoom In"
                  >
                    <ZoomIn className="h-4 w-4" />
                  </button>
                </div>

                {/* Mode Selector and Actions */}
                <div className="flex items-center gap-2">
                  {/* Sepia switcher */}
                  <button
                    onClick={() => setReaderTheme(readerTheme === "sepia" ? "light" : "sepia")}
                    className={`p-1.5 rounded-lg text-xs font-bold transition flex gap-1 items-center border ${
                      readerTheme === "sepia" 
                        ? "bg-amber-100 border-amber-200 text-amber-800" 
                        : "bg-white border-slate-200 hover:bg-slate-50 text-slate-500"
                    }`}
                    title="Sepia Reading Filter"
                  >
                    Sepia
                  </button>

                  {/* Dark mode */}
                  <button
                    onClick={() => setReaderTheme(readerTheme === "dark" ? "light" : "dark")}
                    className={`p-1.5 rounded-lg border transition ${
                      readerTheme === "dark"
                        ? "bg-slate-900 border-slate-950 text-amber-400"
                        : "bg-white border-slate-200 hover:bg-slate-50 text-slate-500"
                    }`}
                    title="Toggle Dark Mode Reading"
                  >
                    {readerTheme === "dark" ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
                  </button>

                  {/* Bookmark Page */}
                  <button
                    onClick={() => handleToggleBookmark(activeBook.id, activePageNum)}
                    className={`p-1.5 rounded-lg border transition ${
                      (bookmarks[activeBook.id] || []).includes(activePageNum)
                        ? "bg-amber-500 text-white border-amber-600 shadow-xs"
                        : "bg-white border-slate-200 hover:bg-slate-50 text-slate-500"
                    }`}
                    title="Bookmark active page"
                  >
                    <Bookmark className="h-4 w-4" />
                  </button>

                  {/* Download raw compiled manual */}
                  <button
                    onClick={() => handleDownloadPdf(activeBook)}
                    className="p-1.5 bg-slate-900 text-white hover:bg-slate-800 rounded-lg transition"
                    title="Download Book TXT manual compilation"
                  >
                    <Download className="h-4 w-4" />
                  </button>
                </div>

              </div>

              {/* Book Paper Canvas Sheet (Preserves scrollability and zoom scale sizes) */}
              <div className="flex-1 p-8 overflow-y-auto flex items-start justify-center">
                <div 
                  className={`shadow-lg border rounded-xl p-8 max-w-3xl w-full min-h-[460px] font-sans leading-relaxed transition-all select-text duration-200 ${
                    readerTheme === "dark" 
                      ? "bg-slate-950 border-slate-800 text-slate-100 shadow-slate-950/20" 
                      : readerTheme === "sepia"
                      ? "bg-amber-50 border-amber-100 text-amber-950"
                      : "bg-white border-slate-200 text-slate-800"
                  }`}
                  style={{ fontSize: `${14 * (zoomLevel / 100)}px` }}
                >
                  <div className="border-b pb-3 mb-6 flex justify-between items-center text-[10px] font-bold uppercase tracking-widest border-current/15 opacity-60">
                    <span>St. Jude Boarding Online Library</span>
                    <span>Page {currentPage.page}</span>
                  </div>

                  <h3 className="text-base font-extrabold font-serif mb-5 tracking-wide underline decoration-current/30">
                    {currentPage.title}
                  </h3>

                  <div className="whitespace-pre-line font-medium text-justify">
                    {highlightSearch(currentPage.content, pdfSearchTerm)}
                  </div>
                </div>
              </div>

            </div>

          </div>
        )}
      </Dialog>

      {/* 2. CATALOG UPLOAD NEW BOOK DIALOG */}
      <Dialog
        isOpen={isUploadOpen}
        onClose={() => setIsUploadOpen(false)}
        title="Add Book to Digital Repository"
      >
        <form onSubmit={handleCreateBook} className="space-y-4 pt-1">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Input
              label="Book Title"
              value={newTitle}
              onChange={(e) => setNewTitle(e.target.value)}
              placeholder="e.g. Advanced Calculus Paradigms"
              required
            />
            <Input
              label="Author Name"
              value={newAuthor}
              onChange={(e) => setNewAuthor(e.target.value)}
              placeholder="e.g. Prof. Alan Turing"
              required
            />
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Select
              label="Subject Category"
              options={[
                { label: "Mathematics", value: "Mathematics" },
                { label: "Science (Physics/Chem/Bio)", value: "Science" },
                { label: "World History", value: "History" },
                { label: "English Literature", value: "Literature" },
                { label: "Computer Science", value: "Computer Science" }
              ]}
              value={newCategory}
              onChange={(e) => setNewCategory(e.target.value)}
            />
            <Select
              label="Target Grade Level"
              options={[
                { label: "All Grade Cohorts", value: "All Grades" },
                { label: "Grade 6", value: "Grade 6" },
                { label: "Grade 7", value: "Grade 7" },
                { label: "Grade 8", value: "Grade 8" },
                { label: "Grade 9", value: "Grade 9" },
                { label: "Grade 10", value: "Grade 10" },
                { label: "Grade 11", value: "Grade 11" },
                { label: "Grade 12", value: "Grade 12" }
              ]}
              value={newGrade}
              onChange={(e) => setNewGrade(e.target.value)}
            />
          </div>

          <div className="grid grid-cols-3 gap-4">
            <Input
              label="ISBN Code (Optional)"
              value={newIsbn}
              onChange={(e) => setNewIsbn(e.target.value)}
              placeholder="e.g. 978-0-12..."
            />
            <Input
              label="Published Year"
              type="number"
              value={newPublishedYear}
              onChange={(e) => setNewPublishedYear(e.target.value)}
              placeholder="2026"
            />
            <Input
              label="Total Pages count"
              type="number"
              value={newPages}
              onChange={(e) => setNewPages(e.target.value)}
              placeholder="150"
            />
          </div>

          <Select
            label="Library Cover Theme Gradient"
            options={gradientOptions}
            value={newGradient}
            onChange={(e) => setNewGradient(e.target.value)}
          />

          <div>
            <label className="text-xs font-bold text-slate-650 tracking-wider block mb-1.5 uppercase">Catalog description</label>
            <textarea
              className="w-full bg-slate-50 border border-slate-205 text-slate-805 rounded-xl p-3 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white max-h-24 transition"
              rows={3}
              placeholder="Provide context about index files, chapters summaries, curricula mapping guidelines..."
              value={newDescription}
              onChange={(e) => setNewDescription(e.target.value)}
            />
          </div>

          <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
            <Button variant="outline" onClick={() => setIsUploadOpen(false)}>Cancel Cataloging</Button>
            <Button type="submit" className="bg-slate-900 hover:bg-slate-800 font-extrabold text-white">Publish to Repositories</Button>
          </div>
        </form>
      </Dialog>
    </div>
  );
}
