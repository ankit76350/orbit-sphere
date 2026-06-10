/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */
import { useState, useEffect } from "react";
import {
  getGalleryAlbums,
  saveGalleryAlbums,
  getGalleryMedia,
  saveGalleryMedia,
  logAction
} from "../storage";
import { Button, Input, Select, Dialog, Badge, useToast } from "../components/ui";
import {
  Tv,
  UploadCloud,
  Plus,
  Image,
  Video,
  Play,
  Trash2,
  Check,
  Eye,
  Calendar,
  X,
  ArrowLeft,
  Search
} from "lucide-react";

export default function ModGallery({ user }) {
  const { addToast } = useToast();

  // Load state databases
  const [albums, setAlbums] = useState(() => getGalleryAlbums());
  const [mediaList, setMediaList] = useState(() => getGalleryMedia());

  // Active Tab: showcase, live_stream, approvals
  const [activeTab, setActiveTab] = useState("showcase");

  // Selected Album for detail view (null means grid of albums)
  const [selectedAlbumId, setSelectedAlbumId] = useState(null);

  // Dialog toggles
  const [isUploadMediaOpen, setIsUploadMediaOpen] = useState(false);
  const [isCreateAlbumOpen, setIsCreateAlbumOpen] = useState(false);

  // Form states
  // 1. Create Album
  const [albumTitle, setAlbumTitle] = useState("");
  const [albumEvent, setAlbumEvent] = useState("Annual Day");
  const [albumCover, setAlbumCover] = useState("");

  // 2. Upload Media
  const [uploadTargetAlbumId, setUploadTargetAlbumId] = useState("");
  const [uploadedUrl, setUploadedUrl] = useState("");
  const [mediaType, setMediaType] = useState("Photo");

  // Filters & Search
  const [galleryFilter, setGalleryFilter] = useState("All");
  const [albumSearch, setAlbumSearch] = useState("");

  // Lightbox & Video Player modal states
  const [activePhotoLightbox, setActivePhotoLightbox] = useState(null);
  const [activeVideoPlayer, setActiveVideoPlayer] = useState(null);

  // Role permissions validation
  const isSuperAdmin = user?.role === "Super Admin";
  const isTeacher = user?.role === "Teacher" || user?.role === "Super Admin";
  const isParent = user?.role === "Parent";
  const isStudent = user?.role === "Student" || user?.role === "Parent";

  useEffect(() => {
    if (albums.length > 0 && !uploadTargetAlbumId) {
      setUploadTargetAlbumId(albums[0].id);
    }
  }, [albums]);

  // HANDLERS
  // 1. Create Album
  const handleCreateAlbum = (e) => {
    e.preventDefault();
    if (!albumTitle.trim()) {
      addToast("Failed to Create", "Album title is required.", "error");
      return;
    }

    const isDirectlyApproved = isSuperAdmin || user?.role === "Principal";
    const newAlbum = {
      id: `alb-${Date.now()}`,
      title: albumTitle.trim(),
      eventType: albumEvent,
      coverImage: albumCover.trim() || "https://images.unsplash.com/photo-1511578314322-379afb476865",
      status: isDirectlyApproved ? "Approved" : "Pending"
    };

    const nextAlbums = [...albums, newAlbum];
    setAlbums(nextAlbums);
    saveGalleryAlbums(nextAlbums);

    logAction(
      user?.id || "sandbox",
      user?.name || "User",
      user?.role || "Staff",
      "Media Album Created",
      `Created gallery album "${albumTitle}" (Status: ${newAlbum.status})`
    );

    if (isDirectlyApproved) {
      addToast("Album Created", `"${albumTitle}" added and published to school media directory.`);
    } else {
      addToast("Album Submitted", `"${albumTitle}" submitted for Super Admin review and approval.`);
    }

    setIsCreateAlbumOpen(false);
    setAlbumTitle("");
    setAlbumCover("");
  };

  // 2. Upload Media file
  const handleUploadMedia = (e) => {
    e.preventDefault();
    const targetId = uploadTargetAlbumId || (albums.length > 0 ? albums[0].id : "");
    if (!uploadedUrl.trim() || !targetId) {
      addToast("Form Incomplete", "Please specify photo/video source URL and target album.", "error");
      return;
    }

    const newMedia = {
      id: `med-${Date.now()}`,
      albumId: targetId,
      mediaType: mediaType,
      mediaUrl: uploadedUrl.trim()
    };

    const nextMediaList = [...mediaList, newMedia];
    setMediaList(nextMediaList);
    saveGalleryMedia(nextMediaList);

    logAction(
      user?.id || "sandbox",
      user?.name || "User",
      user?.role || "Staff",
      "Media Uploaded",
      `Uploaded ${mediaType} file to album reference: ${targetId}`
    );

    addToast("File Uploaded", `Event media file successfully added to directory.`);
    setIsUploadMediaOpen(false);
    setUploadedUrl("");
  };

  // 3. Media Review Approval (Super Admin)
  const handleApproveAlbum = (albumId) => {
    const updated = albums.map((alb) => {
      if (alb.id === albumId) {
        logAction(
          user?.id || "sandbox",
          user?.name || "User",
          user?.role || "Staff",
          "Media Album Approved",
          `Published gallery showcase album: ${alb.title}`
        );
        addToast("Album Approved", `"${alb.title}" is now visible to students & parents.`);
        return { ...alb, status: "Approved" };
      }
      return alb;
    });
    setAlbums(updated);
    saveGalleryAlbums(updated);
  };

  // 4. Reject / Delete Album
  const handleDeleteAlbum = (albumId) => {
    const albName = albums.find(a => a.id === albumId)?.title || "Album";
    const filteredAlbums = albums.filter((alb) => alb.id !== albumId);
    // Also remove associated media files
    const filteredMedia = mediaList.filter((med) => med.albumId !== albumId);

    setAlbums(filteredAlbums);
    setMediaList(filteredMedia);

    saveGalleryAlbums(filteredAlbums);
    saveGalleryMedia(filteredMedia);

    logAction(
      user?.id || "sandbox",
      user?.name || "User",
      user?.role || "Staff",
      "Media Album Deleted",
      `Removed gallery album and all associated media: ${albName}`
    );

    addToast("Album Removed", `"${albName}" has been deleted successfully.`);
    if (selectedAlbumId === albumId) {
      setSelectedAlbumId(null);
    }
  };

  // 5. Delete specific media file
  const handleDeleteMedia = (mediaId) => {
    const filteredMedia = mediaList.filter((med) => med.id !== mediaId);
    setMediaList(filteredMedia);
    saveGalleryMedia(filteredMedia);

    logAction(
      user?.id || "sandbox",
      user?.name || "User",
      user?.role || "Staff",
      "Media File Deleted",
      `Removed media item: ${mediaId}`
    );

    addToast("Media Removed", `Selected media file has been removed.`);
  };

  // Filtering Logic
  const filteredAlbums = albums.filter((alb) => {
    const matchesSearch = alb.title.toLowerCase().includes(albumSearch.toLowerCase()) ||
                          alb.eventType.toLowerCase().includes(albumSearch.toLowerCase());
    const isApproved = alb.status === "Approved";
    return matchesSearch && isApproved;
  });

  const activeAlbum = albums.find((alb) => alb.id === selectedAlbumId);
  const activeAlbumMedia = mediaList.filter((med) => {
    const matchesAlbum = med.albumId === selectedAlbumId;
    const matchesFormat = galleryFilter === "All" || med.mediaType === galleryFilter;
    return matchesAlbum && matchesFormat;
  });

  return (
    <div className="space-y-6">
      {/* Header Banner */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 bg-white p-5 rounded-3xl border border-slate-100 shadow-xs relative overflow-hidden">
        <div className="absolute right-0 top-0 -mt-12 -mr-12 h-36 w-36 bg-blue-500 rounded-full blur-3xl opacity-10 pointer-events-none" />
        <div>
          <h2 className="text-xl font-extrabold text-slate-800 tracking-tight flex items-center gap-2">
            <Image className="h-6 w-6 text-indigo-650 animate-pulse" />
            School Media Gallery & Event Showcase
          </h2>
          <p className="text-xs text-slate-400 font-semibold mt-1">
            Browse campus achievements, decadal annual days, sports tournaments, and check in on live event streams.
          </p>
        </div>

        <div className="flex items-center gap-2 bg-slate-50 border border-slate-150 px-3 py-1.5 rounded-xl">
          <Badge variant="secondary" className="text-[10px] font-mono font-black uppercase">
            ROLE: {user?.role || "Visitor"}
          </Badge>
        </div>
      </div>

      {/* Tabs Menu */}
      <div className="flex gap-1.5 bg-slate-100 p-1.5 rounded-2xl overflow-x-auto scrollbar-none">
        <button
          onClick={() => {
            setActiveTab("showcase");
            setSelectedAlbumId(null);
          }}
          className={`px-4 py-2.5 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${
            activeTab === "showcase" ? "bg-white text-blue-700 shadow-xs" : "text-slate-500 hover:text-slate-800"
          }`}
        >
          Media Showcase
        </button>
        <button
          onClick={() => setActiveTab("live_stream")}
          className={`px-4 py-2.5 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${
            activeTab === "live_stream" ? "bg-white text-blue-700 shadow-xs" : "text-slate-500 hover:text-slate-800"
          }`}
        >
          Live Webcasts
        </button>
        {isSuperAdmin && (
          <button
            onClick={() => setActiveTab("approvals")}
            className={`px-4 py-2.5 text-xs font-bold rounded-xl transition cursor-pointer whitespace-nowrap ${
              activeTab === "approvals" ? "bg-white text-blue-700 shadow-xs" : "text-slate-500 hover:text-slate-800"
            }`}
          >
            Review Portal
            {albums.filter((a) => a.status === "Pending").length > 0 && (
              <span className="ml-1.5 px-1.5 py-0.5 text-[9px] font-black bg-rose-500 text-white rounded-full">
                {albums.filter((a) => a.status === "Pending").length}
              </span>
            )}
          </button>
        )}
      </div>

      {/* ========================================================
          TAB 1: SHOWCASE
          ======================================================== */}
      {activeTab === "showcase" && (
        <div className="space-y-6">
          {/* Controls Bar */}
          <div className="bg-white p-4 rounded-2xl border border-slate-100 flex flex-col md:flex-row justify-between items-center gap-4">
            <div className="flex items-center gap-2 w-full md:w-auto">
              {selectedAlbumId && (
                <button
                  onClick={() => setSelectedAlbumId(null)}
                  className="p-2 bg-slate-50 hover:bg-slate-100 border border-slate-200 text-slate-600 rounded-xl transition flex items-center justify-center cursor-pointer shrink-0"
                  title="Back to Albums"
                >
                  <ArrowLeft className="h-4 w-4" />
                </button>
              )}
              <div>
                <h3 className="text-xs font-black text-slate-805 uppercase tracking-widest">
                  {selectedAlbumId ? `Album: ${activeAlbum?.title}` : "Showcase Albums"}
                </h3>
                <p className="text-[11px] text-slate-400 font-semibold mt-1">
                  {selectedAlbumId
                    ? `${activeAlbum?.eventType} • Showing ${activeAlbumMedia.length} media files`
                    : "Discover, browse, and curate academic event archives."}
                </p>
              </div>
            </div>

            <div className="flex flex-wrap gap-3 w-full md:w-auto justify-end">
              {/* Formatter Filter (Only visible when viewing an album) */}
              {selectedAlbumId && (
                <div className="w-36">
                  <Select
                    options={[
                      { label: "All Formats", value: "All" },
                      { label: "Photos Only", value: "Photo" },
                      { label: "Videos Only", value: "Video" }
                    ]}
                    value={galleryFilter}
                    onChange={(e) => setGalleryFilter(e.target.value)}
                    className="text-xs h-9 py-1 bg-slate-50 rounded-xl border border-slate-200"
                  />
                </div>
              )}

              {/* Search Field (Only visible on album grid) */}
              {!selectedAlbumId && (
                <div className="relative w-48">
                  <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-slate-400" />
                  <input
                    type="text"
                    placeholder="Search albums..."
                    value={albumSearch}
                    onChange={(e) => setAlbumSearch(e.target.value)}
                    className="bg-slate-50 border border-slate-200 rounded-xl pl-8 pr-4 py-1.5 text-xs focus:outline-none w-full transition font-semibold"
                  />
                </div>
              )}

              {isTeacher && (
                <>
                  <Button
                    onClick={() => setIsCreateAlbumOpen(true)}
                    className="text-xs py-1 px-4 bg-white border border-slate-200 text-slate-700 hover:bg-slate-50 rounded-xl flex items-center gap-1.5"
                  >
                    <Plus className="h-4 w-4 shrink-0" /> Create Album
                  </Button>
                  <Button
                    onClick={() => {
                      if (selectedAlbumId) {
                        setUploadTargetAlbumId(selectedAlbumId);
                      }
                      setIsUploadMediaOpen(true);
                    }}
                    className="text-xs py-1 px-4 bg-blue-600 hover:bg-blue-700 text-white rounded-xl shadow-xs flex items-center gap-1.5"
                  >
                    <UploadCloud className="h-4 w-4 shrink-0" /> Upload Media
                  </Button>
                </>
              )}
            </div>
          </div>

          {/* Album Directory (SelectedAlbumId === null) */}
          {!selectedAlbumId ? (
            filteredAlbums.length === 0 ? (
              <div className="bg-white border border-slate-100 rounded-3xl p-12 text-center text-slate-400 italic">
                No showcase albums found matching criteria.
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {filteredAlbums.map((alb) => {
                  const mediaCount = mediaList.filter((m) => m.albumId === alb.id).length;
                  return (
                    <div
                      key={alb.id}
                      onClick={() => setSelectedAlbumId(alb.id)}
                      className="bg-white border border-slate-100 rounded-3xl overflow-hidden shadow-2xs hover:shadow-md transition duration-200 flex flex-col group cursor-pointer"
                    >
                      {/* Album Cover Photo */}
                      <div className="h-44 w-full bg-slate-100 overflow-hidden relative">
                        <img
                          src={alb.coverImage}
                          alt={alb.title}
                          className="h-full w-full object-cover group-hover:scale-105 transition duration-300"
                          onError={(e) => {
                            e.target.src = "https://images.unsplash.com/photo-1511578314322-379afb476865?w=500";
                          }}
                        />
                        <div className="absolute top-3 right-3 bg-black/60 text-white text-[10px] font-mono px-2 py-0.5 rounded-lg font-bold">
                          {mediaCount} files
                        </div>
                      </div>

                      {/* Info details */}
                      <div className="p-5 flex-1 flex flex-col justify-between">
                        <div className="space-y-1.5">
                          <span className="text-[9px] uppercase tracking-wider font-extrabold text-blue-650">
                            {alb.eventType}
                          </span>
                          <h4 className="text-sm font-black text-slate-800 line-clamp-2 group-hover:text-blue-600 transition">
                            {alb.title}
                          </h4>
                        </div>

                        {/* Actions */}
                        <div className="pt-4 mt-4 border-t border-slate-50 flex justify-between items-center">
                          <span className="text-[10px] font-bold text-slate-400 font-mono">
                            ID: {alb.id}
                          </span>
                          <div className="flex gap-2" onClick={(e) => e.stopPropagation()}>
                            {isTeacher && (
                              <button
                                onClick={() => handleDeleteAlbum(alb.id)}
                                className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition shrink-0"
                                title="Delete Album"
                              >
                                <Trash2 className="h-4 w-4" />
                              </button>
                            )}
                            <button
                              onClick={() => setSelectedAlbumId(alb.id)}
                              className="text-xs text-blue-650 font-bold hover:underline flex items-center gap-1"
                            >
                              Explore <Eye className="h-3.5 w-3.5" />
                            </button>
                          </div>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            )
          ) : (
            /* Album Detail View (SelectedAlbumId !== null) */
            <div className="space-y-6">
              {activeAlbumMedia.length === 0 ? (
                <div className="bg-white border border-slate-100 rounded-3xl p-12 text-center text-slate-400 italic">
                  This album is currently empty. Upload photos or videos to showcase them here!
                </div>
              ) : (
                <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
                  {activeAlbumMedia.map((med) => {
                    const isVideo = med.mediaType === "Video";
                    return (
                      <div
                        key={med.id}
                        className="bg-white border border-slate-150 rounded-2xl overflow-hidden shadow-3xs hover:shadow-xs transition duration-200 group relative"
                      >
                        {/* Media preview block */}
                        <div
                          onClick={() => {
                            if (isVideo) {
                              setActiveVideoPlayer(med);
                            } else {
                              setActivePhotoLightbox(med);
                            }
                          }}
                          className="h-40 w-full bg-slate-900 cursor-pointer overflow-hidden relative flex items-center justify-center"
                        >
                          <img
                            src={med.mediaUrl}
                            alt="Media item"
                            className="h-full w-full object-cover group-hover:scale-105 transition duration-300"
                            onError={(e) => {
                              e.target.src = isVideo
                                ? "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?w=500" // Sports/Video placeholder
                                : "https://images.unsplash.com/photo-1511578314322-379afb476865?w=500";
                            }}
                          />

                          {/* Hover Overlay */}
                          <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition duration-200 flex items-center justify-center">
                            {isVideo ? (
                              <div className="h-10 w-10 bg-white/95 rounded-full flex items-center justify-center shadow-lg text-slate-900 scale-90 group-hover:scale-100 transition duration-200">
                                <Play className="h-5 w-5 fill-current ml-0.5" />
                              </div>
                            ) : (
                              <span className="text-[10px] text-white font-mono bg-black/50 px-2 py-1 rounded-md font-bold uppercase">
                                View Photo
                              </span>
                            )}
                          </div>

                          {/* Media Type Icon Badge */}
                          <div className="absolute top-2 left-2 bg-black/60 text-white p-1 rounded-lg">
                            {isVideo ? (
                              <Video className="h-3.5 w-3.5 text-blue-400" />
                            ) : (
                              <Image className="h-3.5 w-3.5 text-indigo-400" />
                            )}
                          </div>
                        </div>

                        {/* Media Card Footer with deletion */}
                        {isTeacher && (
                          <div className="p-2 bg-slate-50 border-t border-slate-100 flex justify-between items-center">
                            <span className="text-[9px] font-mono text-slate-400 truncate">
                              ID: {med.id.split("-")[1] || med.id}
                            </span>
                            <button
                              onClick={() => handleDeleteMedia(med.id)}
                              className="p-1 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-md transition"
                              title="Delete Media File"
                            >
                              <Trash2 className="h-3.5 w-3.5" />
                            </button>
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {/* ========================================================
          TAB 2: LIVE WEBCASTS
          ======================================================== */}
      {activeTab === "live_stream" && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          {/* Main Webcast Viewport */}
          <div className="lg:col-span-8 bg-slate-950 border border-slate-900 rounded-3xl p-6 flex flex-col justify-between min-h-[440px] relative overflow-hidden shadow-md">
            {/* Overlay Grid / Scanlines */}
            <div className="absolute inset-0 bg-scanlines opacity-[0.08] pointer-events-none" />
            <div className="absolute inset-0 bg-noise opacity-[0.04] pointer-events-none" />

            <div className="flex justify-between items-center text-[10px] text-white font-mono bg-black/60 px-3 py-1.5 rounded-xl z-10">
              <span className="uppercase font-bold tracking-widest flex items-center gap-1.5">
                <span className="h-2 w-2 rounded-full bg-rose-500 animate-ping" />
                Live Broadcast: St. Jude's Annual Day Gala
              </span>
              <span className="text-slate-400">FPS: 60 | Secure Stream Feed</span>
            </div>

            <div className="flex-1 flex flex-col items-center justify-center my-8 relative">
              <div className="absolute inset-0 bg-radial-gradient from-blue-500/10 to-transparent blur-3xl rounded-full" />
              <Tv className="h-16 w-16 text-slate-700 animate-pulse relative z-10" />
              <span className="text-xs text-slate-500 font-mono font-bold mt-4 uppercase tracking-widest relative z-10">
                Broadcasting Feed Standby...
              </span>
              <p className="text-[10px] text-slate-650 max-w-sm text-center mt-2 font-semibold">
                Webcast will auto-initialize when the auditorium board is flipped active.
              </p>
            </div>

            <div className="text-[10px] text-slate-400 font-bold uppercase tracking-wider text-center border-t border-slate-900 pt-3 relative z-10 flex items-center justify-center gap-2">
              <span className="h-1.5 w-1.5 rounded-full bg-indigo-500" />
              Real-time watermark encoding and strict screenshot blocks are active for child privacy compliance.
            </div>
          </div>

          {/* Webcast Bulletins & Details */}
          <div className="lg:col-span-4 bg-white border border-slate-100 rounded-3xl p-6 space-y-4 shadow-2xs h-fit">
            <h3 className="text-xs font-black text-slate-805 uppercase tracking-widest border-b pb-3 border-slate-50">
              Broadcast Agenda
            </h3>

            <div className="space-y-4 font-semibold text-xs leading-relaxed">
              <div className="p-3 bg-slate-50 border border-slate-100 rounded-xl space-y-1">
                <Badge variant="rose" className="text-[8px] font-black uppercase">Next Stream</Badge>
                <h4 className="text-xs font-bold text-slate-800 mt-1">Inter-House Football Finals</h4>
                <p className="text-[10px] text-slate-400">June 14, 2026 • 09:30 AM</p>
              </div>

              <div className="p-3 bg-slate-50 border border-slate-100 rounded-xl space-y-1">
                <Badge variant="secondary" className="text-[8px] font-black uppercase">Archived Webinar</Badge>
                <h4 className="text-xs font-bold text-slate-800 mt-1">Virtual Parents-Teacher Assembly</h4>
                <p className="text-[10px] text-slate-400">Duration: 1.5 Hours • Recorded</p>
              </div>

              <div className="p-4 bg-blue-50/10 border border-blue-200 rounded-2xl text-[11px] text-blue-700">
                Authorized guardians can access school-wide broadcasts directly from this hub. Links are protected by multi-tenant security layers.
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ========================================================
          TAB 3: ADMIN APPROVALS
          ======================================================== */}
      {activeTab === "approvals" && isSuperAdmin && (
        <div className="bg-white border border-slate-100 rounded-3xl p-6 space-y-4 shadow-2xs">
          <div>
            <h3 className="text-xs font-black text-slate-805 uppercase tracking-widest">
              Pending Media Albums Review
            </h3>
            <p className="text-[11px] text-slate-400 font-semibold mt-1">
              Review and approve pending media showcase albums submitted by staff.
            </p>
          </div>

          <div className="space-y-4 pt-2">
            {albums.filter((alb) => alb.status === "Pending").length === 0 ? (
              <div className="p-8 text-center text-slate-400 italic border border-dashed border-slate-200 rounded-2xl">
                No media albums are currently pending approval reviews.
              </div>
            ) : (
              albums
                .filter((alb) => alb.status === "Pending")
                .map((alb) => (
                  <div
                    key={alb.id}
                    className="p-4 bg-slate-50 border border-slate-100 rounded-2xl flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 hover:border-slate-200 transition"
                  >
                    <div className="flex gap-3 items-center">
                      <div className="h-10 w-10 bg-amber-50 text-amber-600 rounded-xl flex items-center justify-center shrink-0">
                        <Image className="h-5 w-5" />
                      </div>
                      <div>
                        <h4 className="text-xs font-black text-slate-800">{alb.title}</h4>
                        <span className="text-[9px] font-mono text-slate-450 uppercase tracking-wider block mt-0.5">
                          Category: {alb.eventType}
                        </span>
                      </div>
                    </div>

                    <div className="flex gap-2 shrink-0 w-full sm:w-auto justify-end">
                      <Button
                        onClick={() => handleDeleteAlbum(alb.id)}
                        className="text-[10px] py-1 h-8 px-3.5 bg-white border border-slate-200 hover:bg-slate-50 text-slate-600 rounded-lg font-bold"
                      >
                        Reject & Drop
                      </Button>
                      <Button
                        onClick={() => handleApproveAlbum(alb.id)}
                        className="text-[10px] py-1 h-8 px-4 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg font-bold shadow-2xs"
                      >
                        Approve & Publish
                      </Button>
                    </div>
                  </div>
                ))
            )}
          </div>
        </div>
      )}

      {/* ========================================================
          LIGHTBOX & PLAYER DIALOGS
          ======================================================== */}
      {/* 1. Photo Lightbox Modal */}
      {activePhotoLightbox && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/85 backdrop-blur-xs p-4 animate-fade-in"
          onClick={() => setActivePhotoLightbox(null)}
        >
          <div
            className="relative max-w-4xl w-full max-h-[85vh] flex flex-col items-center justify-center"
            onClick={(e) => e.stopPropagation()}
          >
            {/* Close Button */}
            <button
              onClick={() => setActivePhotoLightbox(null)}
              className="absolute -top-10 right-0 p-1.5 bg-white/10 hover:bg-white/20 text-white rounded-full transition cursor-pointer"
            >
              <X className="h-6 w-6" />
            </button>

            {/* Lightbox Image */}
            <img
              src={activePhotoLightbox.mediaUrl}
              alt="Enlarged Showcase view"
              className="max-w-full max-h-[75vh] object-contain rounded-lg shadow-2xl"
              onError={(e) => {
                e.target.src = "https://images.unsplash.com/photo-1511578314322-379afb476865?w=1200";
              }}
            />

            {/* Details Footer */}
            <div className="mt-4 text-center text-white/90">
              <p className="text-xs font-semibold font-mono bg-black/40 px-3 py-1.5 rounded-full inline-block">
                File Reference: {activePhotoLightbox.id}
              </p>
            </div>
          </div>
        </div>
      )}

      {/* 2. Video Player Modal */}
      {activeVideoPlayer && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/85 backdrop-blur-xs p-4"
          onClick={() => setActiveVideoPlayer(null)}
        >
          <div
            className="relative max-w-3xl w-full bg-slate-950 rounded-2xl border border-slate-800 overflow-hidden shadow-2xl flex flex-col"
            onClick={(e) => e.stopPropagation()}
          >
            {/* Header */}
            <div className="p-4 bg-slate-900 border-b border-slate-850 flex justify-between items-center text-white">
              <h4 className="text-xs font-black uppercase tracking-wider flex items-center gap-2">
                <Video className="h-4 w-4 text-blue-400" /> Recorded Event Stream
              </h4>
              <button
                onClick={() => setActiveVideoPlayer(null)}
                className="p-1 hover:bg-slate-800 text-slate-400 hover:text-white rounded-lg transition"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            {/* Simulated Player Viewport */}
            <div className="h-96 w-full flex flex-col justify-between p-4 relative bg-black">
              <div className="flex-1 flex flex-col items-center justify-center text-white/70">
                <Play className="h-16 w-16 text-blue-500 animate-pulse mb-3" />
                <span className="text-xs font-mono font-bold uppercase tracking-widest text-slate-300">
                  Simulating Media playback...
                </span>
                <p className="text-[10px] text-slate-500 mt-2 font-mono break-all max-w-sm text-center">
                  Source: {activeVideoPlayer.mediaUrl}
                </p>
              </div>

              {/* Progress bar */}
              <div className="w-full space-y-2 mt-4">
                <div className="w-full bg-slate-800 h-1.5 rounded-full overflow-hidden">
                  <div className="bg-blue-500 h-full w-1/3 rounded-full" />
                </div>
                <div className="flex justify-between items-center text-[9px] font-mono text-slate-400">
                  <span>02:14 / 06:40</span>
                  <span>1080p WebStream</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ========================================================
          CREATION & UPLOAD MODALS
          ======================================================== */}
      {/* 1. Create Album Modal */}
      <Dialog
        isOpen={isCreateAlbumOpen}
        onClose={() => setIsCreateAlbumOpen(false)}
        title="Create School Media Album"
      >
        <form onSubmit={handleCreateAlbum} className="space-y-4 pt-1">
          <Input
            label="Album Title *"
            value={albumTitle}
            onChange={(e) => setAlbumTitle(e.target.value)}
            placeholder="Science Fair 2026 Showcase"
            required
          />

          <Select
            label="Event Category Type"
            options={[
              { label: "Annual Day Gala", value: "Annual Day" },
              { label: "Sports Day Championship", value: "Sports Day" },
              { label: "Science Exhibition Panel", value: "Science Exhibition" },
              { label: "Hostel Activities", value: "Hostel Activities" },
              { label: "Cultural Program", value: "Cultural Program" }
            ]}
            value={albumEvent}
            onChange={(e) => setAlbumEvent(e.target.value)}
          />

          <Input
            label="Cover Image URL (Optional)"
            value={albumCover}
            onChange={(e) => setAlbumCover(e.target.value)}
            placeholder="https://images.unsplash.com/..."
          />

          <div className="p-4 bg-indigo-50/10 border border-indigo-200 rounded-2xl text-[11px] text-indigo-700 leading-relaxed font-bold">
            Creating a media album allows parents and students to browse photos and videos from that event. Direct approval requires Admin role permissions.
          </div>

          <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
            <Button variant="outline" size="sm" onClick={() => setIsCreateAlbumOpen(false)}>
              Cancel
            </Button>
            <Button variant="secondary" size="sm" type="submit">
              Register Album
            </Button>
          </div>
        </form>
      </Dialog>

      {/* 2. Upload Media Modal */}
      <Dialog
        isOpen={isUploadMediaOpen}
        onClose={() => setIsUploadMediaOpen(false)}
        title="Upload Media file to Event Album"
      >
        <form onSubmit={handleUploadMedia} className="space-y-4 pt-1">
          <Select
            label="Target Showcase Album *"
            options={albums.map((alb) => ({
              label: alb.title,
              value: alb.id
            }))}
            value={uploadTargetAlbumId}
            onChange={(e) => setUploadTargetAlbumId(e.target.value)}
            required
          />

          <div className="grid grid-cols-2 gap-4">
            <Select
              label="Media File Type"
              options={[
                { label: "Photo", value: "Photo" },
                { label: "Video", value: "Video" }
              ]}
              value={mediaType}
              onChange={(e) => setMediaType(e.target.value)}
            />
            <Input
              label="Media URL Link *"
              value={uploadedUrl}
              onChange={(e) => setUploadedUrl(e.target.value)}
              placeholder="https://images.unsplash.com/..."
              required
            />
          </div>

          <div className="flex gap-3 justify-end pt-3 border-t border-slate-100">
            <Button variant="outline" size="sm" onClick={() => setIsUploadMediaOpen(false)}>
              Cancel
            </Button>
            <Button variant="secondary" size="sm" type="submit">
              Upload Media File
            </Button>
          </div>
        </form>
      </Dialog>
    </div>
  );
}
