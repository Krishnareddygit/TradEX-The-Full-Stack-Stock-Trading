import { useEffect, useState } from "react"
import { getNotifications, markAllRead } from "../services/api"
import { useToast } from "../context/ToastContext"
import { Bell, CheckCheck, TrendingUp, TrendingDown } from "lucide-react"

import "./Notifications.css"

export default function Notifications() {

const [notifications,setNotifications] = useState([])
const [loading,setLoading] = useState(true)

const toast = useToast()

const load = () => {

getNotifications()
.then(r=>setNotifications(r.data))
.finally(()=>setLoading(false))

}

useEffect(()=>{ load() },[])


const handleMarkAll = async()=>{

try{

await markAllRead()
toast.success("All notifications marked as read")
load()

}catch{
toast.error("Failed to mark read")
}

}


const unread = notifications.filter(n=>!n.read).length


if(loading){

return(
<div className="page">
<div className="spinner"/>
</div>
)

}


return(

<div className="notifications-page">


{/* HEADER */}

<div className="notifications-header">

<div>

<h1>
<Bell size={20}/>
Notifications
</h1>

{unread>0 && (
<span className="unread-count">
{unread} unread
</span>
)}

</div>


{unread>0 && (

<button className="mark-read-btn" onClick={handleMarkAll}>

<CheckCheck size={16}/>
Mark All Read

</button>

)}

</div>



{/* FEED */}

{notifications.length === 0 ? (

<div className="empty">

<Bell size={40}/>

<p>No notifications yet</p>

<span>Trade activity will appear here</span>

</div>

) : (

<div className="notifications-feed">

{notifications.map(n=>{

const msg = n.message?.toLowerCase() || ""

const isBuy =
  msg.includes("bought") ||
  msg.includes("margin buy")

return(

<div
key={n.id}
className={`notification-card ${!n.read?"unread":""}`}
>

<div className={`icon ${isBuy?"buy":"sell"}`}>

{isBuy
? <TrendingUp size={16}/>
: <TrendingDown size={16}/>
}

</div>


<div className="content">

<div className="message">

{n.message}

</div>

<div className="time">

{n.createdAt
? new Date(n.createdAt).toLocaleString("en-IN")
: ""}

</div>

</div>

</div>

)

})}

</div>

)}

</div>

)

}