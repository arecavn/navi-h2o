# navi-h2o
Initital h2o project of navigosgroup, using sparkling-water to do text classification on VNW jobs.
(Improving and updating ...)

Training: average error rate is 0.02 -> precision is 98%


![Training confusion matrix](https://github.com/arecavn/navi-h2o/blob/master/images/trainning-confusion-matrix.png)
![Training confusion matrix total](https://github.com/arecavn/navi-h2o/blob/master/images/trainning-confusion-matrix-total.png)

Validation: average error rate is 0.057 -> precision is 94.3%

![Training confusion matrix](https://github.com/arecavn/navi-h2o/blob/master/images/validation-confusion-matrix.png)
![Training confusion matrix total](https://github.com/arecavn/navi-h2o/blob/master/images/validation-confusion-matrix-total.png)


*** Input data sample for training and validation :
classifiedRole,jobTitle
Mobile Developer,Mobile Security Manager/assistant Manager
Mobile Developer,Mobile Software Product Development Manager/assistant Manager
Engineering Manager,PC SW Lead Engineer
Mobile Developer,Mobile Web App Manager/assistant Manager
Engineering Manager,Patent Manager/assistant Manager
Mobile Developer,Mac Os/ios SW Lead Engineer
Sales Manager,IT Sales Executive

*** Input data sample for prediction
Project Manager
Senior Java Developer
03 GRAPHIC Designers - Thiết Kế Đồ Họa ($600 - $1000/ Month)
Software Business Analyst
Experienced PHP Developers
IT Networking
Cán Bộ Tư Vấn Triển Khai Phần Mềm Kế Toán, ERP

*** Predicted result sample
Project Manager ==> Engineering Manager:0.8194240922634218[: 0.000, Account Manager: 0.000, Backend Developer: 0.002, Business Development: 0.020, Content Creator: 0.000, Creative Director: 0.000, Data Scientist: 0.001, Designer: 0.002, DevOps: 0.000, Developer: 0.005, Devops: 0.009, Engineering Manager: 0.819, Frontend Developer: 0.001, Full Stack Developer: 0.001, Manager Business Development: 0.133, Marketing: 0.001, Mobile Developer: 0.001, Product Manager: 0.000, QA Engineer: 0.003, Sales: 0.000, Sales Manager: 0.000, Software Architect: 0.000, UI/UX Designer: 0.000, User Experience Design: 0.000, User Researcher: 0.001, Visual Designer: 0.000]
Senior Java Developer ==> Developer:0.6377226705365414[: 0.000, Account Manager: 0.000, Backend Developer: 0.263, Business Development: 0.001, Content Creator: 0.000, Creative Director: 0.000, Data Scientist: 0.001, Designer: 0.003, DevOps: 0.000, Developer: 0.638, Devops: 0.002, Engineering Manager: 0.002, Frontend Developer: 0.078, Full Stack Developer: 0.002, Manager Business Development: 0.000, Marketing: 0.000, Mobile Developer: 0.006, Product Manager: 0.000, QA Engineer: 0.001, Sales: 0.000, Sales Manager: 0.000, Software Architect: 0.000, UI/UX Designer: 0.000, User Experience Design: 0.000, User Researcher: 0.000, Visual Designer: 0.000]
03 GRAPHIC Designers - Thiết Kế Đồ Họa ($600 - $1000/ Month) ==> Devops:0.578768329512758[: 0.001, Account Manager: 0.001, Backend Developer: 0.015, Business Development: 0.067, Content Creator: 0.002, Creative Director: 0.001, Data Scientist: 0.008, Designer: 0.025, DevOps: 0.001, Developer: 0.198, Devops: 0.579, Engineering Manager: 0.022, Frontend Developer: 0.013, Full Stack Developer: 0.002, Manager Business Development: 0.015, Marketing: 0.002, Mobile Developer: 0.015, Product Manager: 0.001, QA Engineer: 0.021, Sales: 0.006, Sales Manager: 0.001, Software Architect: 0.001, UI/UX Designer: 0.001, User Experience Design: 0.001, User Researcher: 0.002, Visual Designer: 0.005]
Software Business Analyst ==> Developer:0.4970327323542176[: 0.002, Account Manager: 0.004, Backend Developer: 0.050, Business Development: 0.090, Content Creator: 0.005, Creative Director: 0.002, Data Scientist: 0.014, Designer: 0.012, DevOps: 0.002, Developer: 0.497, Devops: 0.048, Engineering Manager: 0.096, Frontend Developer: 0.018, Full Stack Developer: 0.012, Manager Business Development: 0.060, Marketing: 0.003, Mobile Developer: 0.038, Product Manager: 0.002, QA Engineer: 0.023, Sales: 0.006, Sales Manager: 0.002, Software Architect: 0.002, UI/UX Designer: 0.002, User Experience Design: 0.002, User Researcher: 0.003, Visual Designer: 0.004]
Experienced PHP Developers ==> Backend Developer:0.9826455900632379[: 0.000, Account Manager: 0.000, Backend Developer: 0.983, Business Development: 0.001, Content Creator: 0.000, Creative Director: 0.000, Data Scientist: 0.000, Designer: 0.000, DevOps: 0.000, Developer: 0.011, Devops: 0.000, Engineering Manager: 0.001, Frontend Developer: 0.002, Full Stack Developer: 0.000, Manager Business Development: 0.000, Marketing: 0.000, Mobile Developer: 0.000, Product Manager: 0.000, QA Engineer: 0.001, Sales: 0.000, Sales Manager: 0.000, Software Architect: 0.000, UI/UX Designer: 0.000, User Experience Design: 0.000, User Researcher: 0.000, Visual Designer: 0.000]
IT Networking ==> Designer:0.3171471345629595[: 0.001, Account Manager: 0.001, Backend Developer: 0.024, Business Development: 0.037, Content Creator: 0.003, Creative Director: 0.001, Data Scientist: 0.009, Designer: 0.317, DevOps: 0.001, Developer: 0.294, Devops: 0.113, Engineering Manager: 0.041, Frontend Developer: 0.097, Full Stack Developer: 0.004, Manager Business Development: 0.004, Marketing: 0.002, Mobile Developer: 0.022, Product Manager: 0.001, QA Engineer: 0.011, Sales: 0.005, Sales Manager: 0.001, Software Architect: 0.002, UI/UX Designer: 0.001, User Experience Design: 0.001, User Researcher: 0.002, Visual Designer: 0.002]
Cán Bộ Tư Vấn Triển Khai Phần Mềm Kế Toán, ERP ==> Developer:0.7591281668830732[: 0.000, Account Manager: 0.000, Backend Developer: 0.012, Business Development: 0.074, Content Creator: 0.001, Creative Director: 0.000, Data Scientist: 0.012, Designer: 0.005, DevOps: 0.000, Developer: 0.759, Devops: 0.023, Engineering Manager: 0.050, Frontend Developer: 0.012, Full Stack Developer: 0.003, Manager Business Development: 0.005, Marketing: 0.001, Mobile Developer: 0.005, Product Manager: 0.000, QA Engineer: 0.019, Sales: 0.014, Sales Manager: 0.000, Software Architect: 0.000, UI/UX Designer: 0.001, User Experience Design: 0.000, User Researcher: 0.001, Visual Designer: 0.001]




