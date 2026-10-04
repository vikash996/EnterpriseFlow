import ProtectedRoute from "../../components/ProtectedRoute";
import DocumentsManager from "../../components/DocumentsManager";

export default function DocumentsPage() { return <ProtectedRoute><DocumentsManager /></ProtectedRoute>; }
