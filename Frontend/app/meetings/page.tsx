import ProtectedRoute from "../../components/ProtectedRoute";
import MeetingManager from "../../components/MeetingManager";

export default function MeetingsPage() {
  return <ProtectedRoute><MeetingManager /></ProtectedRoute>;
}
